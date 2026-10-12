package com.onlinemenu.billing.dto;

import com.onlinemenu.billing.domain.Currency;
import com.onlinemenu.billing.domain.BillingPeriod;
import com.onlinemenu.billing.domain.PaymentStatus;

import java.time.Instant;
import java.util.Map;

/**
 * @param userEmail filled only in the super admin list
 */
public record PaymentResponseDto(
    long orderNumber,
    String planSlug,
    Map<String, String> planName,
    BillingPeriod period,
    long amount,
    Currency currency,
    String provider,
    PaymentStatus status,
    Instant createdAt,
    Instant paidAt,
    String userEmail
) {

}
