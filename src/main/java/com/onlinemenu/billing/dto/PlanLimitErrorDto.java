package com.onlinemenu.billing.dto;

import com.onlinemenu.billing.domain.LimitKind;

import java.util.Map;

/**
 * Sent with HTTP 402 when a plan limit blocks a request, so the panel can offer a bigger plan.
 *
 * @param suggestedPlan the cheapest plan that allows more, null when there is none
 */
public record PlanLimitErrorDto(
    String limitKey,
    Map<String, String> limitName,
    LimitKind kind,
    Long limit,
    Long used,
    String currentPlan,
    PlanResponseDto suggestedPlan
) {

}
