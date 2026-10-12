package com.onlinemenu.menu.domain;

import com.onlinemenu.cafe.domain.Cafe;
import com.onlinemenu.common.entity.SlugEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "product")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product extends SlugEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "cafe_id", nullable = false, updatable = false)
  private Cafe cafe;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "category_id", nullable = false)
  private MenuCategory category;

  @Builder.Default
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "name_translations", nullable = false, columnDefinition = "jsonb")
  private Map<String, String> nameTranslations = new HashMap<>();

  @Builder.Default
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "description_translations", columnDefinition = "jsonb")
  private Map<String, String> descriptionTranslations = new HashMap<>();

  @Column(name = "price", nullable = false, precision = 12, scale = 2)
  private BigDecimal price;

  /**
   * Free text such as "250 ml" or "300 g".
   */
  @Column(name = "portion", length = 50)
  private String portion;

  @Column(name = "image_url", length = 500)
  private String imageUrl;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  @Column(name = "active", nullable = false)
  private boolean active;

  @Column(name = "available", nullable = false)
  private boolean available;

  @Builder.Default
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "tags", nullable = false, columnDefinition = "jsonb")
  private List<ProductTag> tags = new ArrayList<>();
}
