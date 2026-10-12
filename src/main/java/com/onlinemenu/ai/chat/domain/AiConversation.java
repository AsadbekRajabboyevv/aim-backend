package com.onlinemenu.ai.chat.domain;

import com.onlinemenu.ai.provider.AiToolResult;
import com.onlinemenu.common.entity.SlugEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "ai_conversation")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiConversation extends SlugEntity {

  @Column(name = "owner_id", nullable = false, updatable = false)
  private UUID ownerId;

  @Column(name = "title", length = 200)
  private String title;

  @Column(name = "archived", nullable = false)
  private boolean archived;

  @Column(name = "last_message_at", nullable = false)
  private Instant lastMessageAt;

  @Builder.Default
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "pending_results", nullable = false, columnDefinition = "jsonb")
  private List<AiToolResult> pendingResults = new ArrayList<>();
}
