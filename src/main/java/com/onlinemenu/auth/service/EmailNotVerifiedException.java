package com.onlinemenu.auth.service;

import com.onlinemenu.auth.dto.VerificationSentDto;
import com.onlinemenu.common.constant.ExceptionMessageCodes;
import com.onlinemenu.common.exception.BusinessException;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/** Right password, but the email is not confirmed yet; the client opens the code screen. */
@Getter
public class EmailNotVerifiedException extends BusinessException {

  private final transient VerificationSentDto details;

  public EmailNotVerifiedException(VerificationSentDto details) {
    super(ExceptionMessageCodes.EMAIL_NOT_VERIFIED, HttpStatus.FORBIDDEN);
    this.details = details;
  }
}
