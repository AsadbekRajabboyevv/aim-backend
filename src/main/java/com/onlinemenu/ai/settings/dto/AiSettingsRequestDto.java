package com.onlinemenu.ai.settings.dto;

import com.onlinemenu.common.constant.ExceptionMessageCodes;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record AiSettingsRequestDto(

    boolean enabled,
    @NotBlank(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    String activeProvider,
    Map<String, @Valid Models> models
) {

  public record Models(
      @Size(max = 100, message = ExceptionMessageCodes.VALIDATION_SIZE)
      String model,
      @Size(max = 100, message = ExceptionMessageCodes.VALIDATION_SIZE)
      String titleModel
  ) {

  }
}
