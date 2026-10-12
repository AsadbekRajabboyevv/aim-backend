package com.onlinemenu.auth.dto;

import com.onlinemenu.common.constant.ExceptionMessageCodes;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequestDto(

    @NotBlank(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    @Email(message = ExceptionMessageCodes.VALIDATION_EMAIL_INVALID)
    String email,

    @NotBlank(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    String password
) {

}
