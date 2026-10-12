package com.onlinemenu.ai.chat.dto;

import java.time.Instant;

public record AiConversationSummaryDto(
    String slug,
    String title,
    boolean archived,
    Instant lastMessageAt,
    Instant createdAt
) {

}