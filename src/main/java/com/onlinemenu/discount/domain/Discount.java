package com.onlinemenu.discount.domain;

import com.onlinemenu.cafe.domain.Cafe;
import com.onlinemenu.common.entity.SlugEntity;
import com.onlinemenu.menu.domain.Product;
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

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "discount")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Discount extends SlugEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "cafe_id", nullable = false, updatable = false)
  private Cafe cafe;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @Column(name = "discount_price", nullable = false, precision = 12, scale = 2)
  private BigDecimal discountPrice;

  @Column(name = "starts_at")
  private Instant startsAt;

  @Column(name = "ends_at")
  private Instant endsAt;

  @Column(name = "active", nullable = false)
  private boolean active;
}
