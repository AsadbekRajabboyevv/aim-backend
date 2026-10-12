package com.onlinemenu.user.dto;

import com.onlinemenu.common.constant.ExceptionMessageCodes;
import com.onlinemenu.user.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserRequestDto(

    @NotBlank(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    String fullName,

    @NotBlank(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    @Email(message = ExceptionMessageCodes.VALIDATION_EMAIL_INVALID)
    String email,

    String password,

    @NotNull(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    Role role,

    boolean active
) {

}
