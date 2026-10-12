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
import org.hibernate.annotations.Generated;

import java.time.Instant;
import java.util.UUID;

/**
 * An order for one billing period of a plan, paid through one payment provider.
 */
@Getter
@Setter
@Entity
@Table(name = "billing_payment")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment extends BaseEntity {

  /** Filled by the database sequence; this is the order id the payment systems see. */
  @Generated
  @Column(name = "order_number", insertable = false, updatable = false)
  private Long orderNumber;

  @Column(name = "user_id", nullable = false, updatable = false)
  private UUID userId;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "plan_id", nullable = false, updatable = false)
  private Plan plan;

  @Enumerated(EnumType.STRING)
  @Column(name = "billing_period", nullable = false, length = 20)
  private BillingPeriod period;

  /** In the currency's smallest unit: so'm, or cents for USD/EUR. */
  @Column(name = "amount", nullable = false)
  private long amount;

  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Column(name = "currency", nullable = false, length = 3)
  private Currency currency = Currency.UZS;

  @Column(name = "provider", nullable = false, length = 40)
  private String provider;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private PaymentStatus status;

  @Column(name = "provider_transaction_id", length = 100)
  private String providerTransactionId;

  /** Provider-side creation time in epoch millis (Payme needs it echoed back). */
  @Column(name = "provider_time")
  private Long providerTime;

  @Column(name = "paid_at")
  private Instant paidAt;

  @Column(name = "cancelled_at")
  private Instant cancelledAt;

  @Column(name = "cancel_reason")
  private Integer cancelReason;
}
