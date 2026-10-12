package com.onlinemenu.ai.chat.dto;

import java.time.Instant;
import java.util.List;

public record AiConversationDetailDto(
    String slug,
    String title,
    boolean archived,
    Instant lastMessageAt,
    List<AiChatItemDto> items,
    boolean awaitingConfirmation
) {

}
