package com.onlinemenu.common.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BusinessException {

  public ResourceNotFoundException(String messageCode, Object... args) {
    super(messageCode, HttpStatus.NOT_FOUND, args);
  }
}
