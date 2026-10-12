package com.onlinemenu.ai.chat.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record AiActionDecisionRequestDto(
    boolean approved,
    JsonNode arguments
) {

}
