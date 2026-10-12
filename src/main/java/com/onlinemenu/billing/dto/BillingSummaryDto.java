package com.onlinemenu.billing.dto;

import com.onlinemenu.billing.domain.Currency;
import com.onlinemenu.billing.domain.BillingPeriod;

import java.time.Instant;
import java.util.List;

/**
 * @param enforced false when billing is turned off or the user is not limited (super admin)
 */
public record BillingSummaryDto(
    PlanResponseDto plan,
    SubscriptionState state,
    BillingPeriod period,
    Instant currentPeriodEnd,
    Integer daysLeft,
    List<UsageItemDto> usage,
    UpgradeSuggestionDto suggestion,
    int promptPercent,
    boolean enforced,
    /** The user's billing currency and the country it comes from (null until a Google user picks one). */
    Currency currency,
    String country
) {

}
