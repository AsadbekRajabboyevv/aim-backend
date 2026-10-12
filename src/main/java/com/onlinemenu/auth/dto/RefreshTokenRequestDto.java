package com.onlinemenu.auth.dto;

import com.onlinemenu.common.constant.ExceptionMessageCodes;
import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequestDto(

    @NotBlank(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    String refreshToken
) {

}
