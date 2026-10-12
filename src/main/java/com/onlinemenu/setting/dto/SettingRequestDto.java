package com.onlinemenu.setting.dto;

import com.onlinemenu.common.constant.ExceptionMessageCodes;
import com.onlinemenu.setting.domain.SettingType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SettingRequestDto(

    @NotBlank(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    @Size(max = 1000, message = ExceptionMessageCodes.VALIDATION_SIZE)
    String value,

    SettingType type,

    @Size(max = 500, message = ExceptionMessageCodes.VALIDATION_SIZE)
    String description
) {

}
