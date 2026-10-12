package com.onlinemenu.profile.dto;

import com.onlinemenu.common.constant.ExceptionMessageCodes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordChangeRequestDto(

    String currentPassword,

    @NotBlank(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    @Size(min = 8, max = 100, message = ExceptionMessageCodes.VALIDATION_PASSWORD_SIZE)
    String newPassword
) {

}
