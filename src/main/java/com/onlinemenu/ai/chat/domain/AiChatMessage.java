package com.onlinemenu.ai.chat.domain;

import com.onlinemenu.ai.provider.AiMessage;
import com.onlinemenu.common.entity.BaseEntity;
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

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "ai_message")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiChatMessage extends BaseEntity {

  @Column(name = "conversation_id", nullable = false, updatable = false)
  private UUID conversationId;

  @Column(name = "seq", nullable = false, updatable = false)
  private int seq;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "content", nullable = false, columnDefinition = "jsonb")
  private AiMessage content;
}
