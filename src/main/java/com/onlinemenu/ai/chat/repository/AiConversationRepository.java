package com.onlinemenu.ai.chat.repository;

import com.onlinemenu.ai.chat.domain.AiConversation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiConversationRepository extends JpaRepository<AiConversation, UUID> {

  List<AiConversation> findAllByOwnerIdAndArchivedOrderByLastMessageAtDesc(UUID ownerId,
      boolean archived, Pageable pageable);

  Optional<AiConversation> findBySlugAndOwnerId(String slug, UUID ownerId);

  boolean existsBySlug(String slug);
}
