package com.onlinemenu.legal.dto;

import com.onlinemenu.common.constant.ExceptionMessageCodes;
import jakarta.validation.constraints.NotEmpty;

import java.util.Map;

public record LegalDocumentRequestDto(

    @NotEmpty(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    Map<String, String> titleTranslations,

    @NotEmpty(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    Map<String, String> contentTranslations
) {

}
