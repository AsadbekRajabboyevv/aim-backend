package com.onlinemenu.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;

public record AiToolResult(
    String toolCallId,
    String toolName,
    String content,
    boolean error,
    AiToolOutcome outcome,
    JsonNode confirmedArguments
) {

  public static AiToolResult failed(AiToolCall call, String message) {
    return new AiToolResult(call.id(), call.name(), message, true, AiToolOutcome.FAILED, null);
  }
}
