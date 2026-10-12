package com.onlinemenu.billing.dto;

import com.onlinemenu.billing.domain.BillingPeriod;

import java.time.Instant;
import java.util.Map;

public record SubscriptionAdminDto(
    String userSlug,
    String fullName,
    String email,
    String planSlug,
    Map<String, String> planName,
    SubscriptionState state,
    BillingPeriod period,
    Instant currentPeriodEnd
) {

}
