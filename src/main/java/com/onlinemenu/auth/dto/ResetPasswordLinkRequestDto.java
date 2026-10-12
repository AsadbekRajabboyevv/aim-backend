package com.onlinemenu.auth.dto;

import com.onlinemenu.common.constant.ExceptionMessageCodes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Reset with the token of the emailed link. */
public record ResetPasswordLinkRequestDto(

    @NotBlank(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    String token,

    @NotBlank(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    @Size(min = 8, max = 100, message = ExceptionMessageCodes.VALIDATION_PASSWORD_SIZE)
    String newPassword
) {

}
