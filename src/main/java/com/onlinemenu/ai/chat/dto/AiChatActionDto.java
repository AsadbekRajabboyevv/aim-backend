package com.onlinemenu.ai.chat.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record AiChatActionDto(
    String toolCallId,
    String title,
    JsonNode schema,
    JsonNode values,
    AiChatActionStatus status,
    String message
) {

}