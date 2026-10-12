package com.onlinemenu.ai.chat.service;

public interface AiChatListener {

  AiChatListener NONE = new AiChatListener() {
  };

  default void onConversation(String slug) {
  }

  default void onText(String delta) {
  }

  default void onTool(String label) {
  }
}
