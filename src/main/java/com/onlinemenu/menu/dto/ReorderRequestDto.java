package com.onlinemenu.menu.dto;

import com.onlinemenu.common.constant.ExceptionMessageCodes;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ReorderRequestDto(

    @NotEmpty(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    List<String> slugs
) {

}
