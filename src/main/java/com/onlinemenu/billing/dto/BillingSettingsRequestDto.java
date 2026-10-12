package com.onlinemenu.billing.dto;

import com.onlinemenu.common.constant.ExceptionMessageCodes;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

/**
 * @param providers provider code → offered at checkout
 */
public record BillingSettingsRequestDto(

    boolean enabled,

    @Min(value = 0, message = ExceptionMessageCodes.VALIDATION_SIZE)
    @Max(value = 365, message = ExceptionMessageCodes.VALIDATION_SIZE)
    int trialDays,

    @NotBlank(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    String trialPlan,

    @NotBlank(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    String fallbackPlan,

    @Min(value = 1, message = ExceptionMessageCodes.VALIDATION_SIZE)
    @Max(value = 100, message = ExceptionMessageCodes.VALIDATION_SIZE)
    int promptPercent,

    Map<String, Boolean> providers
) {

}
