package com.onlinemenu.ai.chat.dto;

import com.onlinemenu.common.constant.ExceptionMessageCodes;
import jakarta.validation.constraints.Size;

public record AiConversationUpdateRequestDto(
    @Size(max = 200, message = ExceptionMessageCodes.VALIDATION_SIZE)
    String title,
    Boolean archived
) {

}