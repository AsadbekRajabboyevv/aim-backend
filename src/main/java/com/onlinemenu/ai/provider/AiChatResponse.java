package com.onlinemenu.ai.provider;

import java.util.List;

public record AiChatResponse(
    String text,
    List<AiToolCall> toolCalls,
    AiFinishReason finishReason
) {

  public AiChatResponse {
    toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
  }
}
