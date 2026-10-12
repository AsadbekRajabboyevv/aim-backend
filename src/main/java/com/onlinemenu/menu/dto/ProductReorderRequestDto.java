package com.onlinemenu.menu.dto;

import com.onlinemenu.common.constant.ExceptionMessageCodes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ProductReorderRequestDto(

    @NotBlank(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    String categorySlug,

    @NotEmpty(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    List<String> slugs
) {

}
