package com.onlinemenu.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;

public record AiToolCall(
    String id,
    String name,
    JsonNode arguments,
    JsonNode providerData
) {

  public AiToolCall(String id, String name, JsonNode arguments) {
    this(id, name, arguments, null);
  }
}
