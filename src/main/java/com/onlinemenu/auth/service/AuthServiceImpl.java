package com.onlinemenu.auth.service;

import com.onlinemenu.auth.domain.EmailCodePurpose;
import com.onlinemenu.auth.domain.RefreshToken;
import com.onlinemenu.auth.dto.ForgotPasswordRequestDto;
import com.onlinemenu.auth.dto.LoginRequestDto;
import com.onlinemenu.auth.dto.RegisterRequestDto;
import com.onlinemenu.auth.dto.RefreshTokenRequestDto;
import com.onlinemenu.auth.dto.ResendVerificationRequestDto;
import com.onlinemenu.auth.dto.ResetPasswordLinkRequestDto;
import com.onlinemenu.auth.dto.ResetPasswordRequestDto;
import com.onlinemenu.auth.dto.TelegramConfigDto;
import com.onlinemenu.auth.dto.TokenResponseDto;
import com.onlinemenu.auth.dto.VerificationSentDto;
import com.onlinemenu.auth.dto.VerifyEmailLinkRequestDto;
import com.onlinemenu.auth.dto.VerifyEmailRequestDto;
import com.onlinemenu.auth.repository.RefreshTokenRepository;
import com.onlinemenu.common.constant.ExceptionMessageCodes;
import com.onlinemenu.common.constant.SecurityConstants;
import com.onlinemenu.common.exception.BusinessException;
import com.onlinemenu.common.exception.DuplicateResourceException;
import com.onlinemenu.common.exception.ForbiddenException;
import com.onlinemenu.common.exception.ResourceNotFoundException;
import com.onlinemenu.common.exception.UnauthorizedException;
import com.onlinemenu.common.util.HashUtils;
import com.onlinemenu.common.util.JwtUserClaims;
import com.onlinemenu.common.util.JwtUtils;
import com.onlinemenu.common.util.SlugUtils;
import com.onlinemenu.security.UserPrincipal;
import com.onlinemenu.user.domain.AuthProvider;
import com.onlinemenu.user.domain.Role;
import com.onlinemenu.user.domain.User;
import com.onlinemenu.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

  private final AuthenticationManager authenticationManager;
  private final UserRepository userRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final JwtUtils jwtUtils;
  private final PasswordEncoder passwordEncoder;
  private final EmailVerificationService emailVerificationService;
  private final TelegramAuthVerifier telegramAuthVerifier;

  @Override
  // The code emailed for an unverified account must survive the EmailNotVerifiedException
  @Transactional(noRollbackFor = EmailNotVerifiedException.class)
  public TokenResponseDto login(LoginRequestDto dto) {
    String email = dto.email().trim().toLowerCase();
    // Google-only accounts have no password; tell the user to use Google instead of a generic error
    userRepository.findByEmail(email)
        .filter(user -> user.getPassword() == null)
        .ifPresent(user -> {
          throw new UnauthorizedException(ExceptionMessageCodes.PASSWORD_NOT_SET);
        });
    var authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(email, dto.password()));
    UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
    User user = findUserOrThrow(principal.getSlug());
    if (!user.isEmailVerified()) {
      throw new EmailNotVerifiedException(
          emailVerificationService.send(user, EmailCodePurpose.EMAIL_VERIFICATION, false));
    }
    return issueTokens(user);
  }

  @Override
  @Transactional
  public VerificationSentDto register(RegisterRequestDto dto) {
    String email = dto.email().trim().toLowerCase();
    User user = userRepository.findByEmail(email).orElse(null);
    if (user != null && (user.isEmailVerified() || user.getProvider() != AuthProvider.LOCAL)) {
      throw new DuplicateResourceException(ExceptionMessageCodes.USER_EMAIL_ALREADY_EXISTS, email);
    }
    if (user == null) {
      user = User.builder()
          .email(email)
          .role(Role.CAFE_ADMIN)
          .provider(AuthProvider.LOCAL)
          .emailVerified(false)
          .active(true)
          .build();
      user.setSlug(
          SlugUtils.generateUniqueSlug(dto.fullName().trim(), userRepository::existsBySlug));
    }
    // A sign-up nobody confirmed yet is taken over by the new one; only the inbox owner can finish it
    user.setFullName(dto.fullName().trim());
    user.setPassword(passwordEncoder.encode(dto.password()));
    user.setCountry(dto.country().trim().toUpperCase(Locale.ROOT));
    return emailVerificationService.send(userRepository.save(user),
        EmailCodePurpose.EMAIL_VERIFICATION, false);
  }

  @Override
  // A wrong code must still count as an attempt
  @Transactional(noRollbackFor = BusinessException.class)
  public TokenResponseDto verifyEmail(VerifyEmailRequestDto dto) {
    return issueTokens(emailVerificationService.verifyCode(dto.email(), dto.code(),
        EmailCodePurpose.EMAIL_VERIFICATION));
  }

  @Override
  @Transactional
  public TokenResponseDto verifyEmailLink(VerifyEmailLinkRequestDto dto) {
    return issueTokens(
        emailVerificationService.verifyToken(dto.token(), EmailCodePurpose.EMAIL_VERIFICATION));
  }

  @Override
  @Transactional
  public VerificationSentDto resendVerification(ResendVerificationRequestDto dto) {
    String email = dto.email().trim().toLowerCase(Locale.ROOT);
    return userRepository.findByEmail(email)
        .filter(user -> !user.isEmailVerified())
        .map(user -> emailVerificationService.send(user, EmailCodePurpose.EMAIL_VERIFICATION, true))
        // Same answer for unknown and verified addresses, so this can't be used to probe accounts
        .orElseGet(() -> new VerificationSentDto(email, 0, 0));
  }

  @Override
  @Transactional
  public VerificationSentDto forgotPassword(ForgotPasswordRequestDto dto) {
    String email = dto.email().trim().toLowerCase(Locale.ROOT);
    return userRepository.findByEmail(email)
        .filter(User::isActive)
        // Not strict: a "too soon" error would tell that the account exists
        .map(user -> emailVerificationService.send(user, EmailCodePurpose.PASSWORD_RESET, false))
        .orElseGet(() -> emailVerificationService.notSent(email));
  }

  @Override
  // A wrong code must still count as an attempt
  @Transactional(noRollbackFor = BusinessException.class)
  public TokenResponseDto resetPassword(ResetPasswordRequestDto dto) {
    return setNewPassword(emailVerificationService.verifyCode(dto.email(), dto.code(),
        EmailCodePurpose.PASSWORD_RESET), dto.newPassword());
  }

  @Override
  @Transactional
  public TokenResponseDto resetPasswordLink(ResetPasswordLinkRequestDto dto) {
    return setNewPassword(
        emailVerificationService.verifyToken(dto.token(), EmailCodePurpose.PASSWORD_RESET),
        dto.newPassword());
  }

  private TokenResponseDto setNewPassword(User user, String newPassword) {
    if (!user.isActive()) {
      throw new ForbiddenException(ExceptionMessageCodes.ACCESS_DENIED);
    }
    user.setPassword(passwordEncoder.encode(newPassword));
    refreshTokenRepository.revokeAllByUserId(user.getId());
    return issueTokens(user);
  }

  @Override
  @Transactional
  public TokenResponseDto telegramLogin(Map<String, Object> data) {
    TelegramAuthVerifier.TelegramUser telegramUser = telegramAuthVerifier.verify(data);
    User user = userRepository.findByTelegramId(telegramUser.id())
        .orElseGet(() -> registerTelegramUser(telegramUser));
    if (!user.isActive()) {
      throw new ForbiddenException(ExceptionMessageCodes.ACCESS_DENIED);
    }
    return issueTokens(user);
  }

  @Override
  public TelegramConfigDto telegramConfig() {
    return new TelegramConfigDto(telegramAuthVerifier.getBotUsername());
  }

  @Override
  @Transactional
  public TokenResponseDto refresh(RefreshTokenRequestDto dto) {
    if (!jwtUtils.isValid(dto.refreshToken())
        || !SecurityConstants.TOKEN_TYPE_REFRESH.equals(
        jwtUtils.extractTokenType(dto.refreshToken()))) {
      throw new UnauthorizedException(ExceptionMessageCodes.TOKEN_INVALID);
    }

    String hash = hash(dto.refreshToken());
    RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
        .orElseThrow(
            () -> new UnauthorizedException(ExceptionMessageCodes.REFRESH_TOKEN_NOT_FOUND));

    if (stored.isRevoked() || stored.getExpiresAt().isBefore(Instant.now())) {
      throw new UnauthorizedException(ExceptionMessageCodes.REFRESH_TOKEN_REVOKED);
    }

    stored.setRevoked(true);
    return issueTokens(stored.getUser());
  }

  @Override
  @Transactional
  public void logout(RefreshTokenRequestDto dto) {
    refreshTokenRepository.findByTokenHash(hash(dto.refreshToken()))
        .ifPresent(token -> token.setRevoked(true));
  }

  @Override
  @Transactional
  public TokenResponseDto issueTokensForSlug(String userSlug) {
    return issueTokens(findUserOrThrow(userSlug));
  }

  private User registerTelegramUser(TelegramAuthVerifier.TelegramUser telegramUser) {
    String fullName = Stream.of(telegramUser.firstName(), telegramUser.lastName())
        .filter(StringUtils::hasText)
        .collect(Collectors.joining(" "));
    if (fullName.isBlank()) {
      fullName = StringUtils.hasText(telegramUser.username())
          ? telegramUser.username()
          : "Telegram " + telegramUser.id();
    }
    User user = User.builder()
        .fullName(fullName)
        .role(Role.CAFE_ADMIN)
        .provider(AuthProvider.TELEGRAM)
        .providerId(String.valueOf(telegramUser.id()))
        .telegramId(telegramUser.id())
        .active(true)
        .build();
    user.setSlug(SlugUtils.generateUniqueSlug(fullName, userRepository::existsBySlug));
    return userRepository.save(user);
  }

  private TokenResponseDto issueTokens(User user) {
    JwtUserClaims claims = new JwtUserClaims(
        user.getId(), user.getSlug(), user.getEmail(), user.getRole().name());

    String accessToken = jwtUtils.generateAccessToken(claims);
    String refreshToken = jwtUtils.generateRefreshToken(claims);

    RefreshToken refreshTokenEntity = RefreshToken.builder()
        .user(user)
        .tokenHash(hash(refreshToken))
        .expiresAt(Instant.now().plus(jwtUtils.getRefreshTokenExpirationMs(), ChronoUnit.MILLIS))
        .revoked(false)
        .build();
    refreshTokenRepository.save(refreshTokenEntity);

    return new TokenResponseDto(accessToken, refreshToken, jwtUtils.getAccessTokenExpirationMs(),
        SecurityConstants.TOKEN_PREFIX.trim());
  }

  private User findUserOrThrow(String slug) {
    return userRepository.findBySlug(slug)
        .orElseThrow(
            () -> new ResourceNotFoundException(ExceptionMessageCodes.USER_NOT_FOUND, slug));
  }

  private String hash(String token) {
    return HashUtils.sha256(token);
  }
}
