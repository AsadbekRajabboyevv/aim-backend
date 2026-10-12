package com.onlinemenu.billing.dto;

import java.util.List;

public record BillingSettingsDto(
    boolean enabled,
    int trialDays,
    String trialPlan,
    String fallbackPlan,
    int promptPercent,
    List<ProviderSettingsDto> providers
) {

}
