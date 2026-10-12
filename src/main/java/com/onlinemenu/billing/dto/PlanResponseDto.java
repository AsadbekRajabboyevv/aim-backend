package com.onlinemenu.billing.dto;

import java.util.Map;

public record PlanResponseDto(
    String slug,
    Map<String, String> nameTranslations,
    Map<String, String> descriptionTranslations,
    long monthlyPrice,
    long yearlyPrice,
    long monthlyPriceUsd,
    long yearlyPriceUsd,
    long monthlyPriceEur,
    long yearlyPriceEur,
    Map<String, Long> entitlements,
    int sortOrder,
    boolean recommended,
    boolean active
) {

}
