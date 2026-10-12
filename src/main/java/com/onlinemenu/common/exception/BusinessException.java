package com.onlinemenu.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessException extends RuntimeException {

  private final String messageCode;
  private final HttpStatus status;
  private final transient Object[] args;

  public BusinessException(String messageCode, HttpStatus status, Object... args) {
    super(messageCode);
    this.messageCode = messageCode;
    this.status = status;
    this.args = args;
  }
}
