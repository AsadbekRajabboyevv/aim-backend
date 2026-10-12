package com.onlinemenu.cafe.domain;

import com.onlinemenu.common.entity.SlugEntity;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "cafe")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cafe extends SlugEntity {

  @Builder.Default
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "name_translations", nullable = false, columnDefinition = "jsonb")
  private Map<String, String> nameTranslations = new HashMap<>();

  @Builder.Default
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "description_translations", columnDefinition = "jsonb")
  private Map<String, String> descriptionTranslations = new HashMap<>();

  @Column(name = "logo_url", length = 500)
  private String logoUrl;

  @Column(name = "cover_url", length = 500)
  private String coverUrl;

  @Column(name = "phone", length = 50)
  private String phone;

  @Column(name = "address", length = 500)
  private String address;

  @Column(name = "email", length = 200)
  private String email;

  @Column(name = "working_hours", length = 200)
  private String workingHours;

  @Column(name = "wifi_name", length = 100)
  private String wifiName;

  @Column(name = "wifi_password", length = 100)
  private String wifiPassword;

  @Builder.Default
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "social_links", columnDefinition = "jsonb")
  private Map<String, String> socialLinks = new HashMap<>();

  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Column(name = "theme", nullable = false, length = 30)
  private MenuTheme theme = MenuTheme.ESPRESSO;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "owner_id")
  private User owner;

  @Column(name = "active", nullable = false)
  private boolean active;
}
