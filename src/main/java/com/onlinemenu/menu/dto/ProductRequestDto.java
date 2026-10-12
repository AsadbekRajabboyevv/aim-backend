package com.onlinemenu.menu.dto;

import com.onlinemenu.common.constant.ExceptionMessageCodes;
import com.onlinemenu.menu.domain.ProductTag;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ProductRequestDto(

    @NotBlank(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    String categorySlug,

    @NotEmpty(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    Map<String, String> nameTranslations,

    Map<String, String> descriptionTranslations,

    @NotNull(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    @DecimalMin(value = "0", message = ExceptionMessageCodes.VALIDATION_PRICE_INVALID)
    BigDecimal price,

    @Size(max = 50, message = ExceptionMessageCodes.VALIDATION_SIZE)
    String portion,

    String imageUrl,
    boolean active,
    boolean available,

    List<ProductTag> tags
) {

}
