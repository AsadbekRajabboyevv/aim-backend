package com.onlinemenu.billing.domain;

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

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "billing_plan")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Plan extends SlugEntity {

  public static final long UNLIMITED = -1;

  @Builder.Default
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "name_translations", nullable = false, columnDefinition = "jsonb")
  private Map<String, String> nameTranslations = new HashMap<>();

  @Builder.Default
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "description_translations", columnDefinition = "jsonb")
  private Map<String, String> descriptionTranslations = new HashMap<>();

  /** So'm per month. */
  @Column(name = "monthly_price", nullable = false)
  private long monthlyPrice;

  /** So'm for 12 months. */
  @Column(name = "yearly_price", nullable = false)
  private long yearlyPrice;

  /** Cents per month for payers outside Uzbekistan and the euro area; 0 = not sold in dollars. */
  @Column(name = "monthly_price_usd", nullable = false)
  private long monthlyPriceUsd;

  @Column(name = "yearly_price_usd", nullable = false)
  private long yearlyPriceUsd;

  /** Euro cents per month for payers in the euro area; 0 = not sold in euros. */
  @Column(name = "monthly_price_eur", nullable = false)
  private long monthlyPriceEur;

  @Column(name = "yearly_price_eur", nullable = false)
  private long yearlyPriceEur;

  /** Limit key → value. -1 is unlimited; for FEATURE limits a value above 0 means "on". */
  @Builder.Default
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "entitlements", nullable = false, columnDefinition = "jsonb")
  private Map<String, Long> entitlements = new HashMap<>();

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  @Column(name = "recommended", nullable = false)
  private boolean recommended;

  /** Shown on the pricing page and can be bought. */
  @Column(name = "active", nullable = false)
  private boolean active;

  public long price(BillingPeriod period) {
    return price(period, Currency.UZS);
  }

  /** In the currency's smallest unit (so'm, cents); 0 when the plan has no price in it. */
  public long price(BillingPeriod period, Currency currency) {
    boolean yearly = period == BillingPeriod.YEARLY;
    return switch (currency) {
      case UZS -> yearly ? yearlyPrice : monthlyPrice;
      case USD -> yearly ? yearlyPriceUsd : monthlyPriceUsd;
      case EUR -> yearly ? yearlyPriceEur : monthlyPriceEur;
    };
  }
}
