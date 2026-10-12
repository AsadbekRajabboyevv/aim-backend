package com.onlinemenu.ai.chat.dto;

import java.time.Instant;

public record AiChatItemDto(
    AiChatItemType type,
    String text,
    String toolName,
    String title,
    AiChatActionDto action,
    Instant createdAt
) {

}