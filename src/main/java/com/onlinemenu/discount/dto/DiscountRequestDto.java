package com.onlinemenu.discount.dto;

import com.onlinemenu.common.constant.ExceptionMessageCodes;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public record DiscountRequestDto(

    @NotBlank(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    String productSlug,

    @NotNull(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    @DecimalMin(value = "0", message = ExceptionMessageCodes.VALIDATION_PRICE_INVALID)
    BigDecimal discountPrice,

    Instant startsAt,

    Instant endsAt,

    boolean active
) {

}
