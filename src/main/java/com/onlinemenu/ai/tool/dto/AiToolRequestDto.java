package com.onlinemenu.ai.tool.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.onlinemenu.ai.tool.domain.AiToolHttpMethod;
import com.onlinemenu.common.constant.ExceptionMessageCodes;
import com.onlinemenu.user.domain.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AiToolRequestDto(

    @NotBlank(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    @Size(max = 64, message = ExceptionMessageCodes.VALIDATION_SIZE)
    String name,

    @NotBlank(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    @Size(max = 150, message = ExceptionMessageCodes.VALIDATION_SIZE)
    String title,

    @NotBlank(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    @Size(max = 4000, message = ExceptionMessageCodes.VALIDATION_SIZE)
    String description,

    @NotNull(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    AiToolHttpMethod httpMethod,

    @NotBlank(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    @Size(max = 300, message = ExceptionMessageCodes.VALIDATION_SIZE)
    String pathTemplate,

    @NotNull(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    JsonNode inputSchema,

    boolean requiresConfirmation,

    @NotEmpty(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    List<Role> allowedRoles,

    boolean active
) {

}
