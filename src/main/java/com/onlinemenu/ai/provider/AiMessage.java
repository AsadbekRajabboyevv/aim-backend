package com.onlinemenu.ai.provider;

import java.util.List;

public record AiMessage(
    AiRole role,
    String text,
    List<AiToolCall> toolCalls,
    List<AiToolResult> toolResults
) {

  public AiMessage {
    toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
    toolResults = toolResults == null ? List.of() : List.copyOf(toolResults);
  }

  public static AiMessage user(String text) {
    return new AiMessage(AiRole.USER, text, List.of(), List.of());
  }

  public static AiMessage user(String text, List<AiToolResult> toolResults) {
    return new AiMessage(AiRole.USER, text, List.of(), toolResults);
  }

  public static AiMessage assistant(String text, List<AiToolCall> toolCalls) {
    return new AiMessage(AiRole.ASSISTANT, text, toolCalls, List.of());
  }

  public boolean hasText() {
    return text != null && !text.isBlank();
  }
}
