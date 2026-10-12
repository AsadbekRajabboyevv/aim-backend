package com.onlinemenu.ai.provider;

import java.util.List;
import java.util.function.Consumer;

public interface AiChatProvider {

  String id();

  String displayName();

  String defaultModel();

  String defaultTitleModel();

  boolean isConfigured();

  AiChatResponse chat(AiChatRequest request);

  default AiChatResponse chatStream(AiChatRequest request, Consumer<String> onText) {
    AiChatResponse response = chat(request);
    if (response.text() != null && !response.text().isEmpty()) {
      onText.accept(response.text());
    }
    return response;
  }

  default List<String> listModels() {
    return List.of();
  }
}
