package com.onlinemenu.auth.service;

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

import java.util.Map;

public interface AuthService {

  TokenResponseDto login(LoginRequestDto dto);

  /** Creates the account and emails a verification code; tokens come after {@link #verifyEmail}. */
  VerificationSentDto register(RegisterRequestDto dto);

  TokenResponseDto verifyEmail(VerifyEmailRequestDto dto);

  TokenResponseDto verifyEmailLink(VerifyEmailLinkRequestDto dto);

  VerificationSentDto resendVerification(ResendVerificationRequestDto dto);

  /** Emails a password reset code and link; answers the same whether or not the account exists. */
  VerificationSentDto forgotPassword(ForgotPasswordRequestDto dto);

  /** Sets the new password, signs out every other session and signs the user in. */
  TokenResponseDto resetPassword(ResetPasswordRequestDto dto);

  TokenResponseDto resetPasswordLink(ResetPasswordLinkRequestDto dto);

  /** Signs in (or signs up) with the data of the Telegram Login Widget. */
  TokenResponseDto telegramLogin(Map<String, Object> data);

  TelegramConfigDto telegramConfig();

  TokenResponseDto refresh(RefreshTokenRequestDto dto);

  void logout(RefreshTokenRequestDto dto);

  TokenResponseDto issueTokensForSlug(String userSlug);
}
