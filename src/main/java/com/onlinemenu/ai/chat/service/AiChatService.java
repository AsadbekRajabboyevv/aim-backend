package com.onlinemenu.ai.chat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.onlinemenu.ai.chat.dto.AiConversationDetailDto;
import com.onlinemenu.ai.chat.dto.AiConversationSummaryDto;
import com.onlinemenu.ai.chat.dto.AiConversationUpdateRequestDto;
import com.onlinemenu.ai.chat.dto.AiStatusDto;

import java.util.List;

public interface AiChatService {

  AiStatusDto status(AiChatContext context);

  List<AiConversationSummaryDto> get(AiChatContext context, boolean archived);

  AiConversationDetailDto get(AiChatContext context, String slug);

  AiConversationDetailDto create(AiChatContext context, String text, AiChatListener listener);

  AiConversationDetailDto send(AiChatContext context, String slug, String text,
      AiChatListener listener);

  AiConversationDetailDto decide(AiChatContext context, String slug, String toolCallId,
      boolean approved,
      JsonNode arguments, AiChatListener listener);

  AiConversationDetailDto decideAll(AiChatContext context, String slug, boolean approved,
      AiChatListener listener);

  AiConversationSummaryDto update(AiChatContext context, String slug,
      AiConversationUpdateRequestDto dto);

  void delete(AiChatContext context, String slug);

  default AiConversationDetailDto create(AiChatContext context, String text) {
    return create(context, text, AiChatListener.NONE);
  }

  default AiConversationDetailDto send(AiChatContext context, String slug, String text) {
    return send(context, slug, text, AiChatListener.NONE);
  }

  default AiConversationDetailDto decide(AiChatContext context, String slug, String toolCallId,
      boolean approved, JsonNode arguments) {
    return decide(context, slug, toolCallId, approved, arguments, AiChatListener.NONE);
  }

  default AiConversationDetailDto decideAll(AiChatContext context, String slug, boolean approved) {
    return decideAll(context, slug, approved, AiChatListener.NONE);
  }
}
