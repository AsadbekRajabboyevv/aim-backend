package com.onlinemenu.billing.dto;

import com.onlinemenu.billing.domain.Currency;

import java.util.List;

public record PlanCatalogDto(
    List<PlanResponseDto> plans,
    List<LimitDefinitionResponseDto> limits,
    /** The currency prices are shown in for this viewer. */
    Currency currency
) {

}
