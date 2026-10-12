package com.onlinemenu.billing.dto;

/**
 * @param limitKey the limit that triggered the suggestion, null for trial and expiry reasons
 */
public record UpgradeSuggestionDto(
    UpgradeReason reason,
    String limitKey,
    PlanResponseDto plan
) {

}
