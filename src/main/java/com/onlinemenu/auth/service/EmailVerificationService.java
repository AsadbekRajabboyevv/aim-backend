package com.onlinemenu.auth.service;

import com.onlinemenu.auth.domain.EmailCodePurpose;
import com.onlinemenu.auth.dto.VerificationSentDto;
import com.onlinemenu.user.domain.User;

/** Emailed 6-digit codes and links: email verification and password reset. */
public interface EmailVerificationService {

  /**
   * Emails a new code and link unless one was sent within the cooldown; with {@code strict} the
   * cooldown is an error instead of a silent skip.
   */
  VerificationSentDto send(User user, EmailCodePurpose purpose, boolean strict);

  /**
   * Accepts the code (and marks the email verified, since the user proved they read the inbox), or
   * fails with the reason it is not accepted.
   */
  User verifyCode(String email, String code, EmailCodePurpose purpose);

  User verifyToken(String token, EmailCodePurpose purpose);

  /** What {@link #send} answers, for addresses that get no email; keeps accounts from being probed. */
  VerificationSentDto notSent(String email);
}
