package com.onlinemenu.common.exception;

import org.springframework.http.HttpStatus;

public class DuplicateResourceException extends BusinessException {

  public DuplicateResourceException(String messageCode, Object... args) {
    super(messageCode, HttpStatus.CONFLICT, args);
  }
}
