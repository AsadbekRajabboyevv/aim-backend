package com.onlinemenu.billing.dto;

import com.onlinemenu.billing.domain.LimitKind;
import com.onlinemenu.billing.domain.LimitPeriod;

import java.util.Map;

/**
 * @param enforced whether the platform actually blocks going over this limit (built-in code, a
 *                 counter or a path pattern); otherwise it is only shown on the pricing page
 */
public record LimitDefinitionResponseDto(
    String key,
    Map<String, String> nameTranslations,
    LimitKind kind,
    LimitPeriod period,
    String httpMethod,
    String pathPattern,
    long defaultValue,
    int sortOrder,
    boolean builtIn,
    boolean enforced
) {

}
