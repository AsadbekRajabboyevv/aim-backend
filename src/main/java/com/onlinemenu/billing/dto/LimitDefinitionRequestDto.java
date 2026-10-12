package com.onlinemenu.billing.dto;

import com.onlinemenu.billing.domain.LimitKind;
import com.onlinemenu.billing.domain.LimitPeriod;
import com.onlinemenu.common.constant.ExceptionMessageCodes;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * @param key only read on create; keys cannot change because plans refer to them
 */
public record LimitDefinitionRequestDto(

    @Size(max = 64, message = ExceptionMessageCodes.VALIDATION_SIZE)
    @Pattern(regexp = "^[a-z0-9]+([._][a-z0-9]+)*$", message = ExceptionMessageCodes.VALIDATION_SETTING_KEY_FORMAT)
    String key,

    @NotEmpty(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    Map<String, String> nameTranslations,

    @NotNull(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    LimitKind kind,

    LimitPeriod period,

    @Size(max = 10, message = ExceptionMessageCodes.VALIDATION_SIZE)
    String httpMethod,

    @Size(max = 300, message = ExceptionMessageCodes.VALIDATION_SIZE)
    String pathPattern,

    Long defaultValue,

    Integer sortOrder
) {

}
