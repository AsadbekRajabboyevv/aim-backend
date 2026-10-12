package com.onlinemenu.common.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends BusinessException {

  public ForbiddenException(String messageCode, Object... args) {
    super(messageCode, HttpStatus.FORBIDDEN, args);
  }
}
