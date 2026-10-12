package com.onlinemenu.user.domain;

import com.onlinemenu.common.entity.SlugEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "app_user")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends SlugEntity {

  @Column(name = "full_name", nullable = false, length = 200)
  private String fullName;

  /** Null for Telegram sign-ups. */
  @Column(name = "email", unique = true, length = 200)
  private String email;

  /** Password sign-ups can't log in until they confirm the email; Google and older accounts are verified. */
  @Column(name = "email_verified", nullable = false)
  private boolean emailVerified;

  @Column(name = "telegram_id", unique = true)
  private Long telegramId;

  @Column(name = "password")
  private String password;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false, length = 30)
  private Role role;

  @Enumerated(EnumType.STRING)
  @Column(name = "provider", nullable = false, length = 20)
  private AuthProvider provider;

  @Column(name = "provider_id")
  private String providerId;

  @Column(name = "active", nullable = false)
  private boolean active;

  /** ISO 3166-1 alpha-2; decides the billing currency. Null for Google sign-ups until they pick one. */
  @Column(name = "country", length = 2)
  private String country;
}
