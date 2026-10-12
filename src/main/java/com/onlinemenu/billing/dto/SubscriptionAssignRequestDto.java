package com.onlinemenu.billing.dto;

import com.onlinemenu.billing.domain.BillingPeriod;
import com.onlinemenu.common.constant.ExceptionMessageCodes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Super admin sets a plan by hand (cash, bank transfer, a gift).
 *
 * @param period null makes it a trial
 */
public record SubscriptionAssignRequestDto(

    @NotBlank(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    String planSlug,

    BillingPeriod period,

    @NotNull(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    Instant currentPeriodEnd
) {

}
