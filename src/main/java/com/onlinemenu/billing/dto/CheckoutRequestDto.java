package com.onlinemenu.billing.dto;

import com.onlinemenu.billing.domain.BillingPeriod;
import com.onlinemenu.common.constant.ExceptionMessageCodes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CheckoutRequestDto(

    @NotBlank(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    String planSlug,

    @NotNull(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    BillingPeriod period,

    @NotBlank(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    String provider
) {

}
