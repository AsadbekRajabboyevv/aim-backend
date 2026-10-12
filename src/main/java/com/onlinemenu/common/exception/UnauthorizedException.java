package com.onlinemenu.common.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends BusinessException {

  public UnauthorizedException(String messageCode, Object... args) {
    super(messageCode, HttpStatus.UNAUTHORIZED, args);
  }
}
