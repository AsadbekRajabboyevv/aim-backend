package com.onlinemenu.auth.controller;

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
import com.onlinemenu.auth.service.AuthService;
import com.onlinemenu.common.dto.BaseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @PostMapping("/login")
  public BaseResponse<TokenResponseDto> login(@Valid @RequestBody LoginRequestDto dto) {
    return BaseResponse.success(authService.login(dto));
  }

  @PostMapping("/register")
  public BaseResponse<VerificationSentDto> register(@Valid @RequestBody RegisterRequestDto dto) {
    return BaseResponse.success(authService.register(dto));
  }

  @PostMapping("/verify-email")
  public BaseResponse<TokenResponseDto> verifyEmail(@Valid @RequestBody VerifyEmailRequestDto dto) {
    return BaseResponse.success(authService.verifyEmail(dto));
  }

  @PostMapping("/verify-email/link")
  public BaseResponse<TokenResponseDto> verifyEmailLink(
      @Valid @RequestBody VerifyEmailLinkRequestDto dto) {
    return BaseResponse.success(authService.verifyEmailLink(dto));
  }

  @PostMapping("/verify-email/resend")
  public BaseResponse<VerificationSentDto> resendVerification(
      @Valid @RequestBody ResendVerificationRequestDto dto) {
    return BaseResponse.success(authService.resendVerification(dto));
  }

  @PostMapping("/password/forgot")
  public BaseResponse<VerificationSentDto> forgotPassword(
      @Valid @RequestBody ForgotPasswordRequestDto dto) {
    return BaseResponse.success(authService.forgotPassword(dto));
  }

  @PostMapping("/password/reset")
  public BaseResponse<TokenResponseDto> resetPassword(
      @Valid @RequestBody ResetPasswordRequestDto dto) {
    return BaseResponse.success(authService.resetPassword(dto));
  }

  @PostMapping("/password/reset/link")
  public BaseResponse<TokenResponseDto> resetPasswordLink(
      @Valid @RequestBody ResetPasswordLinkRequestDto dto) {
    return BaseResponse.success(authService.resetPasswordLink(dto));
  }

  @GetMapping("/telegram/config")
  public BaseResponse<TelegramConfigDto> telegramConfig() {
    return BaseResponse.success(authService.telegramConfig());
  }

  /** Body: the user object the Telegram Login Widget passes to its callback, unchanged. */
  @PostMapping("/telegram")
  public BaseResponse<TokenResponseDto> telegramLogin(@RequestBody Map<String, Object> data) {
    return BaseResponse.success(authService.telegramLogin(data));
  }

  @PostMapping("/refresh")
  public BaseResponse<TokenResponseDto> refresh(@Valid @RequestBody RefreshTokenRequestDto dto) {
    return BaseResponse.success(authService.refresh(dto));
  }

  @PostMapping("/logout")
  public BaseResponse<Void> logout(@Valid @RequestBody RefreshTokenRequestDto dto) {
    authService.logout(dto);
    return BaseResponse.success(null);
  }
}
