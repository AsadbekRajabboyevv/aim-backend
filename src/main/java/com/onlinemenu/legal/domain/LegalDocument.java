package com.onlinemenu.legal.domain;

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

import java.util.HashMap;
import java.util.Map;

/**
 * A public legal page (Terms of use, Privacy policy). The text is Markdown, one per language,
 * edited by the super admin.
 */
@Getter
@Setter
@Entity
@Table(name = "legal_document")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LegalDocument extends BaseEntity {

  @Column(name = "slug", nullable = false, unique = true, length = 50)
  private String slug;

  @Builder.Default
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "title_translations", nullable = false, columnDefinition = "jsonb")
  private Map<String, String> titleTranslations = new HashMap<>();

  @Builder.Default
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "content_translations", nullable = false, columnDefinition = "jsonb")
  private Map<String, String> contentTranslations = new HashMap<>();
}
