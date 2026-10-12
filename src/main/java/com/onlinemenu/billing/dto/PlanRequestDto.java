package com.onlinemenu.billing.dto;

import com.onlinemenu.common.constant.ExceptionMessageCodes;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.Map;

/**
 * @param entitlements limit key → value; -1 is unlimited, a missing key uses the limit's default
 */
public record PlanRequestDto(

    @NotEmpty(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    Map<String, String> nameTranslations,

    Map<String, String> descriptionTranslations,

    @NotNull(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    @PositiveOrZero(message = ExceptionMessageCodes.VALIDATION_PRICE_INVALID)
    Long monthlyPrice,

    @NotNull(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    @PositiveOrZero(message = ExceptionMessageCodes.VALIDATION_PRICE_INVALID)
    Long yearlyPrice,

    /** Cents; null or 0 = not sold in that currency. */
    @PositiveOrZero(message = ExceptionMessageCodes.VALIDATION_PRICE_INVALID)
    Long monthlyPriceUsd,

    @PositiveOrZero(message = ExceptionMessageCodes.VALIDATION_PRICE_INVALID)
    Long yearlyPriceUsd,

    @PositiveOrZero(message = ExceptionMessageCodes.VALIDATION_PRICE_INVALID)
    Long monthlyPriceEur,

    @PositiveOrZero(message = ExceptionMessageCodes.VALIDATION_PRICE_INVALID)
    Long yearlyPriceEur,

    Map<String, Long> entitlements,

    Integer sortOrder,

    boolean recommended,

    boolean active
) {

}
