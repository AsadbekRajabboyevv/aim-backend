package com.onlinemenu.auth.dto;

import com.onlinemenu.common.constant.ExceptionMessageCodes;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Reset with the emailed code. */
public record ResetPasswordRequestDto(

    @NotBlank(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    @Email(message = ExceptionMessageCodes.VALIDATION_EMAIL_INVALID)
    String email,

    @NotBlank(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    @Pattern(regexp = "^[0-9]{6}$", message = ExceptionMessageCodes.VALIDATION_VERIFICATION_CODE)
    String code,

    @NotBlank(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    @Size(min = 8, max = 100, message = ExceptionMessageCodes.VALIDATION_PASSWORD_SIZE)
    String newPassword
) {

}
