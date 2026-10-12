package com.onlinemenu.file;

import com.onlinemenu.common.constant.ExceptionMessageCodes;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record DeleteFilesRequestDto(

    @NotEmpty(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    List<String> urls
) {

}
