package com.onlinemenu.ai.tool.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.onlinemenu.ai.tool.domain.AiToolHttpMethod;
import com.onlinemenu.user.domain.Role;

import java.time.Instant;
import java.util.List;

public record AiToolResponseDto(
    String name,
    String title,
    String description,
    AiToolHttpMethod httpMethod,
    String pathTemplate,
    JsonNode inputSchema,
    boolean requiresConfirmation,
    List<Role> allowedRoles,
    boolean active,
    Instant createdAt,
    Instant updatedAt
) {

}
