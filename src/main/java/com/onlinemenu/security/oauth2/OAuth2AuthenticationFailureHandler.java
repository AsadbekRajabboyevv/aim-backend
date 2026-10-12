package com.onlinemenu.security.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * Logs the real OAuth2 failure reason and sends the user back to the frontend callback instead of
 * Spring's default {@code /login?error}, which this stateless API does not serve.
 */
@Slf4j
@Component
public class OAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {

  private static final String DEFAULT_ERROR_CODE = "oauth2_failed";

  @Value("${app.oauth2.redirect-uri}")
  private String redirectUri;

  @Override
  public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
      AuthenticationException exception)
      throws IOException {
    log.warn("Google OAuth2 login failed: {}", exception.getMessage(), exception);

    String errorCode = exception instanceof OAuth2AuthenticationException oauth2Exception
        ? oauth2Exception.getError().getErrorCode()
        : DEFAULT_ERROR_CODE;

    String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
        .queryParam("error", errorCode)
        .build().toUriString();

    response.sendRedirect(targetUrl);
  }
}
