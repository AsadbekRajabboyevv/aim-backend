package com.onlinemenu.auth.domain;

import com.onlinemenu.common.entity.BaseEntity;
import com.onlinemenu.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * A pending emailed confirmation (email verification or password reset): a 6-digit code and a link
 * token, both stored hashed. One per user and purpose.
 */
@Getter
@Setter
@Entity
@Table(name = "email_verification")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailVerification extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(name = "purpose", nullable = false, length = 30)
  private EmailCodePurpose purpose;

  @Column(name = "code_hash", nullable = false, length = 128)
  private String codeHash;

  @Column(name = "token_hash", nullable = false, unique = true, length = 128)
  private String tokenHash;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "sent_at", nullable = false)
  private Instant sentAt;

  /** Wrong codes entered; the code stops working at the configured maximum. */
  @Column(name = "attempts", nullable = false)
  private int attempts;
}
