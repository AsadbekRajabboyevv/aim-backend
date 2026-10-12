package com.onlinemenu.auth.service;

import com.onlinemenu.auth.domain.EmailCodePurpose;
import com.onlinemenu.auth.domain.EmailVerification;
import com.onlinemenu.auth.dto.VerificationSentDto;
import com.onlinemenu.auth.repository.EmailVerificationRepository;
import com.onlinemenu.common.constant.ExceptionMessageCodes;
import com.onlinemenu.common.exception.BusinessException;
import com.onlinemenu.common.util.HashUtils;
import com.onlinemenu.user.domain.User;
import com.onlinemenu.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {

  private static final SecureRandom RANDOM = new SecureRandom();
  private static final int TOKEN_BYTES = 32;

  private final EmailVerificationRepository emailVerificationRepository;
  private final UserRepository userRepository;
  private final VerificationMailSender mailSender;

  @Value("${app.auth.email-verification.ttl-minutes:30}")
  private long ttlMinutes;

  @Value("${app.auth.email-verification.resend-cooldown-seconds:60}")
  private long resendCooldownSeconds;

  @Value("${app.auth.email-verification.max-attempts:5}")
  private int maxAttempts;

  @Override
  @Transactional
  public VerificationSentDto send(User user, EmailCodePurpose purpose, boolean strict) {
    Instant now = Instant.now();
    Optional<EmailVerification> existing =
        emailVerificationRepository.findByUserIdAndPurpose(user.getId(), purpose);
    if (existing.isPresent()) {
      long wait = resendCooldownSeconds
          - Duration.between(existing.get().getSentAt(), now).toSeconds();
      if (wait > 0) {
        if (strict) {
          throw new BusinessException(ExceptionMessageCodes.VERIFICATION_RESEND_TOO_SOON,
              HttpStatus.TOO_MANY_REQUESTS, wait);
        }
        return new VerificationSentDto(user.getEmail(),
            Math.max(0, Duration.between(now, existing.get().getExpiresAt()).toSeconds()), wait);
      }
    }

    String code = "%06d".formatted(RANDOM.nextInt(1_000_000));
    String token = randomToken();
    EmailVerification verification = existing.orElseGet(
        () -> EmailVerification.builder().user(user).purpose(purpose).build());
    verification.setCodeHash(HashUtils.sha256(code));
    verification.setTokenHash(HashUtils.sha256(token));
    verification.setSentAt(now);
    verification.setExpiresAt(now.plus(Duration.ofMinutes(ttlMinutes)));
    verification.setAttempts(0);
    emailVerificationRepository.save(verification);

    mailSender.send(purpose, user.getEmail(), user.getFullName(), code, token, ttlMinutes);
    return new VerificationSentDto(user.getEmail(), ttlMinutes * 60, resendCooldownSeconds);
  }

  @Override
  public VerificationSentDto notSent(String email) {
    return new VerificationSentDto(email, ttlMinutes * 60, resendCooldownSeconds);
  }

  @Override
  @Transactional(noRollbackFor = BusinessException.class)
  public User verifyCode(String email, String code, EmailCodePurpose purpose) {
    User user = userRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
        .orElseThrow(() -> new BusinessException(ExceptionMessageCodes.VERIFICATION_INVALID,
            HttpStatus.BAD_REQUEST));
    if (purpose == EmailCodePurpose.EMAIL_VERIFICATION && user.isEmailVerified()) {
      return user;
    }
    EmailVerification verification = emailVerificationRepository
        .findByUserIdAndPurpose(user.getId(), purpose)
        .orElseThrow(() -> new BusinessException(ExceptionMessageCodes.VERIFICATION_EXPIRED,
            HttpStatus.BAD_REQUEST));
    checkUsable(verification);
    if (!MessageDigest.isEqual(verification.getCodeHash().getBytes(),
        HashUtils.sha256(code.trim()).getBytes())) {
      // Kept despite the exception (noRollbackFor), so guessing runs out of attempts
      verification.setAttempts(verification.getAttempts() + 1);
      throw new BusinessException(ExceptionMessageCodes.VERIFICATION_INVALID,
          HttpStatus.BAD_REQUEST);
    }
    return complete(verification);
  }

  @Override
  @Transactional(noRollbackFor = BusinessException.class)
  public User verifyToken(String token, EmailCodePurpose purpose) {
    EmailVerification verification = emailVerificationRepository.findByTokenHashAndPurpose(
            HashUtils.sha256(token.trim()), purpose)
        .orElseThrow(() -> new BusinessException(ExceptionMessageCodes.VERIFICATION_EXPIRED,
            HttpStatus.BAD_REQUEST));
    if (verification.getExpiresAt().isBefore(Instant.now())) {
      throw new BusinessException(ExceptionMessageCodes.VERIFICATION_EXPIRED,
          HttpStatus.BAD_REQUEST);
    }
    return complete(verification);
  }

  private void checkUsable(EmailVerification verification) {
    if (verification.getExpiresAt().isBefore(Instant.now())) {
      throw new BusinessException(ExceptionMessageCodes.VERIFICATION_EXPIRED,
          HttpStatus.BAD_REQUEST);
    }
    if (verification.getAttempts() >= maxAttempts) {
      throw new BusinessException(ExceptionMessageCodes.VERIFICATION_ATTEMPTS_EXCEEDED,
          HttpStatus.TOO_MANY_REQUESTS);
    }
  }

  private User complete(EmailVerification verification) {
    User user = verification.getUser();
    user.setEmailVerified(true);
    emailVerificationRepository.delete(verification);
    return user;
  }

  private static String randomToken() {
    byte[] bytes = new byte[TOKEN_BYTES];
    RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }
}
