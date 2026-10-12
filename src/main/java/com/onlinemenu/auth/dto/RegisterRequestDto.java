package com.onlinemenu.auth.dto;

import com.onlinemenu.common.constant.ExceptionMessageCodes;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequestDto(

    @NotBlank(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    @Size(max = 200, message = ExceptionMessageCodes.VALIDATION_SIZE)
    String fullName,

    @NotBlank(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    @Email(message = ExceptionMessageCodes.VALIDATION_EMAIL_INVALID)
    @Size(max = 200, message = ExceptionMessageCodes.VALIDATION_SIZE)
    String email,

    @NotBlank(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    @Size(min = 8, max = 100, message = ExceptionMessageCodes.VALIDATION_PASSWORD_SIZE)
    String password,

    @NotBlank(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    @Pattern(regexp = "^[A-Za-z]{2}$", message = ExceptionMessageCodes.VALIDATION_COUNTRY_INVALID)
    String country
) {

}
