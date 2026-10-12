package com.onlinemenu.ai.chat.dto;

import com.onlinemenu.common.constant.ExceptionMessageCodes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiSendMessageRequestDto(
    @NotBlank(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    @Size(max = 4000, message = ExceptionMessageCodes.VALIDATION_SIZE)
    String text
) {

}
