package com.onlinemenu.billing.domain;

import com.onlinemenu.common.entity.BaseEntity;
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
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "billing_subscription")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription extends BaseEntity {

  @Column(name = "user_id", nullable = false, unique = true, updatable = false)
  private UUID userId;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "plan_id", nullable = false)
  private Plan plan;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private SubscriptionStatus status;

  /** Null for a trial. */
  @Enumerated(EnumType.STRING)
  @Column(name = "billing_period", length = 20)
  private BillingPeriod period;

  @Column(name = "current_period_start", nullable = false)
  private Instant currentPeriodStart;

  @Column(name = "current_period_end", nullable = false)
  private Instant currentPeriodEnd;

  /** What the current period cost; unused time is carried over when the plan changes. */
  @Column(name = "paid_amount", nullable = false)
  private long paidAmount;

  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Column(name = "paid_currency", nullable = false, length = 3)
  private Currency paidCurrency = Currency.UZS;

  public boolean isCurrent(Instant now) {
    return currentPeriodEnd.isAfter(now);
  }
}
