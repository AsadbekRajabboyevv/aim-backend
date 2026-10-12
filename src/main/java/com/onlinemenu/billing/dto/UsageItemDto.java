package com.onlinemenu.billing.dto;

import com.onlinemenu.billing.domain.LimitKind;
import com.onlinemenu.billing.domain.LimitPeriod;

import java.util.Map;

/**
 * @param limit   null when unlimited
 * @param used    null for features and for limits nothing counts
 * @param percent share of the limit already used, null when unlimited or not counted
 */
public record UsageItemDto(
    String key,
    Map<String, String> nameTranslations,
    LimitKind kind,
    LimitPeriod period,
    Long limit,
    Long used,
    Integer percent,
    boolean enabled
) {

}
