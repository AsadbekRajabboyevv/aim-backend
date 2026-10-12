package com.onlinemenu.billing.dto;

import com.onlinemenu.billing.domain.Currency;
import com.onlinemenu.billing.domain.BillingPeriod;

import java.time.Instant;

/**
 * What paying for a plan would do.
 *
 * @param renewal   same plan and period: the paid time is added after the current one
 * @param bonusDays unused days of the current paid plan, converted to days of the new plan
 */
public record QuoteDto(
    String planSlug,
    BillingPeriod period,
    long amount,
    Currency currency,
    boolean renewal,
    int bonusDays,
    Instant startsAt,
    Instant endsAt
) {

}
