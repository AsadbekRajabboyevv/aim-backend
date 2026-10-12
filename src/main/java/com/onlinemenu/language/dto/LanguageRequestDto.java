package com.onlinemenu.language.dto;

import com.onlinemenu.common.constant.ExceptionMessageCodes;
import jakarta.validation.constraints.NotBlank;

public record LanguageRequestDto(

    @NotBlank(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    String code,

    @NotBlank(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    String name,

    @NotBlank(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    String nativeName,

    boolean isDefault,
    boolean isActive,
    int sortOrder
) {

}
