package com.onlinemenu.setting.dto;

import com.onlinemenu.common.constant.ExceptionMessageCodes;
import com.onlinemenu.setting.domain.SettingType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SettingCreateRequestDto(

    @NotBlank(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    @Size(max = 150, message = ExceptionMessageCodes.VALIDATION_SIZE)
    @Pattern(regexp = "^[a-z0-9]+([._][a-z0-9]+)*$", message = ExceptionMessageCodes.VALIDATION_SETTING_KEY_FORMAT)
    String key,

    @NotBlank(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    @Size(max = 1000, message = ExceptionMessageCodes.VALIDATION_SIZE)
    String value,

    @NotNull(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    SettingType type,

    @Size(max = 500, message = ExceptionMessageCodes.VALIDATION_SIZE)
    String description
) {

}
