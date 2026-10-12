package com.onlinemenu.auth.dto;

import com.onlinemenu.common.constant.ExceptionMessageCodes;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyEmailRequestDto(

    @NotBlank(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    @Email(message = ExceptionMessageCodes.VALIDATION_EMAIL_INVALID)
    String email,

    @NotBlank(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    @Pattern(regexp = "^[0-9]{6}$", message = ExceptionMessageCodes.VALIDATION_VERIFICATION_CODE)
    String code
) {

}
