package com.onlinemenu.ai.chat.service;

import com.onlinemenu.ai.tool.service.AiToolExecutionContext;
import com.onlinemenu.user.domain.Role;

import java.util.UUID;

public record AiChatContext(
    UUID userId,
    Role role,
    String bearerToken,
    String language
) {

  public boolean isSuperAdmin() {
    return role == Role.SUPER_ADMIN;
  }

  public AiToolExecutionContext toolContext() {
    return new AiToolExecutionContext(bearerToken, language);
  }
}
