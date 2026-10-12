package com.onlinemenu.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinemenu.common.constant.ExceptionMessageCodes;
import com.onlinemenu.common.dto.BaseResponse;
import com.onlinemenu.common.util.ExceptionUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final ExceptionUtils exceptionUtils;
  private final ObjectMapper objectMapper;

  @Override
  public void commence(HttpServletRequest request, HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {
    response.setStatus(HttpStatus.UNAUTHORIZED.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    String message = exceptionUtils.resolveMessage(ExceptionMessageCodes.INVALID_CREDENTIALS);
    objectMapper.writeValue(response.getWriter(), BaseResponse.error(message));
  }
}
