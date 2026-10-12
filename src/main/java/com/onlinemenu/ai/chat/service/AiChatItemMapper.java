package com.onlinemenu.ai.chat.service;

import com.onlinemenu.ai.chat.domain.AiChatMessage;
import com.onlinemenu.ai.chat.domain.AiConversation;
import com.onlinemenu.ai.chat.dto.AiChatActionDto;
import com.onlinemenu.ai.chat.dto.AiChatActionStatus;
import com.onlinemenu.ai.chat.dto.AiChatItemDto;
import com.onlinemenu.ai.chat.dto.AiChatItemType;
import com.onlinemenu.ai.provider.AiMessage;
import com.onlinemenu.ai.provider.AiRole;
import com.onlinemenu.ai.provider.AiToolCall;
import com.onlinemenu.ai.provider.AiToolOutcome;
import com.onlinemenu.ai.provider.AiToolResult;
import com.onlinemenu.ai.tool.domain.AiTool;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
class AiChatItemMapper {

  List<AiChatItemDto> toItems(AiConversation conversation, List<AiChatMessage> messages,
      Map<String, AiTool> toolsByName, boolean superAdmin) {
    Map<String, AiToolResult> resultsById = new HashMap<>();
    for (var message : messages) {
      message.getContent().toolResults()
          .forEach(result -> resultsById.put(result.toolCallId(), result));
    }
    conversation.getPendingResults()
        .forEach(result -> resultsById.put(result.toolCallId(), result));

    List<AiChatItemDto> items = new ArrayList<>();
    for (var message : messages) {
      var content = message.getContent();
      if (content.role() == AiRole.USER) {
        if (content.hasText()) {
          items.add(new AiChatItemDto(AiChatItemType.USER, content.text(), null, null, null,
              message.getCreatedAt()));
        }
        continue;
      }
      if (content.hasText()) {
        items.add(new AiChatItemDto(AiChatItemType.ASSISTANT, content.text(), null, null, null,
            message.getCreatedAt()));
      }
      for (var call : content.toolCalls()) {
        var tool = toolsByName.get(call.name());
        var result = resultsById.get(call.id());
        if (tool != null && tool.isRequiresConfirmation()) {
          items.add(new AiChatItemDto(AiChatItemType.ACTION, null, superAdmin ? call.name() : null,
              tool.getTitle(),
              toAction(call, tool, result), message.getCreatedAt()));
        } else if (superAdmin) {
          items.add(new AiChatItemDto(AiChatItemType.TOOL, null, call.name(),
              tool != null ? tool.getTitle() : call.name(), null, message.getCreatedAt()));
        }
      }
    }
    return items;
  }

  private AiChatActionDto toAction(AiToolCall call, AiTool tool, AiToolResult result) {
    if (result == null) {
      return new AiChatActionDto(call.id(), tool.getTitle(), tool.getInputSchema(),
          call.arguments(), AiChatActionStatus.PENDING, null);
    }
    var status = switch (result.outcome() != null ? result.outcome() : AiToolOutcome.EXECUTED) {
      case EXECUTED -> AiChatActionStatus.EXECUTED;
      case FAILED -> AiChatActionStatus.FAILED;
      case CANCELLED -> AiChatActionStatus.CANCELLED;
    };
    return new AiChatActionDto(call.id(), tool.getTitle(), tool.getInputSchema(),
        result.confirmedArguments() != null ? result.confirmedArguments() : call.arguments(),
        status, status == AiChatActionStatus.FAILED ? userMessage(result) : null);
  }

  private String userMessage(AiToolResult result) {
    String content = result.content();
    int colon = content.indexOf("): ");
    return colon >= 0 ? content.substring(colon + 3) : content;
  }
}
