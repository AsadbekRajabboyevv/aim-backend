package com.onlinemenu.profile.dto;

import com.onlinemenu.common.constant.ExceptionMessageCodes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProfileRequestDto(

    @NotBlank(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    @Size(max = 200, message = ExceptionMessageCodes.VALIDATION_SIZE)
    String fullName,

    /** ISO 3166-1 alpha-2; null keeps the current country. */
    @Pattern(regexp = "^[A-Za-z]{2}$", message = ExceptionMessageCodes.VALIDATION_COUNTRY_INVALID)
    String country
) {

}
