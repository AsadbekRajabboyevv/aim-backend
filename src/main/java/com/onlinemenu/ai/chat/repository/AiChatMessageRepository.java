package com.onlinemenu.ai.chat.repository;

import com.onlinemenu.ai.chat.domain.AiChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface AiChatMessageRepository extends JpaRepository<AiChatMessage, UUID> {

  List<AiChatMessage> findAllByConversationIdOrderBySeqAsc(UUID conversationId);

}
