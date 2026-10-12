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

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "menu_category")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuCategory extends SlugEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "cafe_id", nullable = false, updatable = false)
  private Cafe cafe;

  @Builder.Default
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "name_translations", nullable = false, columnDefinition = "jsonb")
  private Map<String, String> nameTranslations = new HashMap<>();

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  @Column(name = "active", nullable = false)
  private boolean active;
}
