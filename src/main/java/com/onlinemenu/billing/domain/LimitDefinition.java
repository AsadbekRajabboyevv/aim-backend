package com.onlinemenu.billing.domain;

import com.onlinemenu.common.entity.BaseEntity;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.HashMap;
import java.util.Map;

/**
 * A limit plans can set. Built-in ones are enforced in code; the rest are enforced by matching API
 * requests against {@link #pathPattern}, or only shown on the pricing page when there is no
 * pattern.
 */
@Getter
@Setter
@Entity
@Table(name = "billing_limit_definition")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LimitDefinition extends BaseEntity {

  @Column(name = "limit_key", nullable = false, unique = true, updatable = false, length = 64)
  private String key;

  @Builder.Default
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "name_translations", nullable = false, columnDefinition = "jsonb")
  private Map<String, String> nameTranslations = new HashMap<>();

  @Enumerated(EnumType.STRING)
  @Column(name = "kind", nullable = false, length = 20)
  private LimitKind kind;

  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Column(name = "period", nullable = false, length = 20)
  private LimitPeriod period = LimitPeriod.NONE;

  /** Null matches any method. */
  @Column(name = "http_method", length = 10)
  private String httpMethod;

  /** Ant pattern of API paths this limit guards, e.g. /api/v1/cafes/{@literal *}/discounts. */
  @Column(name = "path_pattern", length = 300)
  private String pathPattern;

  /** Value for plans that do not set this limit. */
  @Column(name = "default_value", nullable = false)
  private long defaultValue;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  @Column(name = "built_in", nullable = false)
  private boolean builtIn;
}
