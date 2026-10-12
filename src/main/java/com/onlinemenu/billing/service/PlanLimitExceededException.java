package com.onlinemenu.billing.service;

import com.onlinemenu.billing.dto.PlanLimitErrorDto;
import com.onlinemenu.common.exception.BusinessException;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * A plan limit blocked the request. Answered with HTTP 402 and the details, so the panel can
 * offer a bigger plan.
 */
@Getter
public class PlanLimitExceededException extends BusinessException {

  private final transient PlanLimitErrorDto details;

  public PlanLimitExceededException(String messageCode, PlanLimitErrorDto details,
      Object... args) {
    super(messageCode, HttpStatus.PAYMENT_REQUIRED, args);
    this.details = details;
  }
}
