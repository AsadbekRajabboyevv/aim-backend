package com.onlinemenu.systemmessage.dto;

import com.onlinemenu.common.constant.ExceptionMessageCodes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.Map;

public record SystemMessageRequestDto(

    @NotBlank(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    String code,

    @NotEmpty(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    Map<String, String> translations
) {

}
