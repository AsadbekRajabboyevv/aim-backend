package com.onlinemenu.security.oauth2;

import com.onlinemenu.auth.dto.TokenResponseDto;
import com.onlinemenu.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

  private final AuthService authService;

  @Value("${app.oauth2.redirect-uri}")
  private String redirectUri;

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
      Authentication authentication)
      throws IOException {
    OAuth2UserPrincipalAdapter principal = (OAuth2UserPrincipalAdapter) authentication.getPrincipal();
    TokenResponseDto tokens = authService.issueTokensForSlug(
        principal.getUserPrincipal().getSlug());

    String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
        .queryParam("accessToken", tokens.accessToken())
        .queryParam("refreshToken", tokens.refreshToken())
        .build().toUriString();

    response.sendRedirect(targetUrl);
  }
}
