package com.onlinemenu.menu.dto;

import com.onlinemenu.common.constant.ExceptionMessageCodes;
import jakarta.validation.constraints.NotEmpty;

import java.util.Map;

public record MenuCategoryRequestDto(

    @NotEmpty(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    Map<String, String> nameTranslations,

    boolean active
) {

}
