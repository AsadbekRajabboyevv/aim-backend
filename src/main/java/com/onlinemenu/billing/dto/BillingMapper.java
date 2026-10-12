package com.onlinemenu.billing.dto;

import com.onlinemenu.billing.domain.LimitDefinition;
import com.onlinemenu.billing.domain.Payment;
import com.onlinemenu.billing.domain.Plan;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class BillingMapper {

  public PlanResponseDto toDto(Plan plan) {
    return new PlanResponseDto(plan.getSlug(), plan.getNameTranslations(),
        plan.getDescriptionTranslations() == null ? Map.of() : plan.getDescriptionTranslations(),
        plan.getMonthlyPrice(), plan.getYearlyPrice(), plan.getMonthlyPriceUsd(), plan.getYearlyPriceUsd(),
        plan.getMonthlyPriceEur(), plan.getYearlyPriceEur(), new HashMap<>(plan.getEntitlements()),
        plan.getSortOrder(), plan.isRecommended(), plan.isActive());
  }

  public LimitDefinitionResponseDto toDto(LimitDefinition definition, boolean enforced) {
    return new LimitDefinitionResponseDto(definition.getKey(), definition.getNameTranslations(),
        definition.getKind(), definition.getPeriod(), definition.getHttpMethod(),
        definition.getPathPattern(), definition.getDefaultValue(), definition.getSortOrder(),
        definition.isBuiltIn(), enforced);
  }

  public PaymentResponseDto toDto(Payment payment, String userEmail) {
    return new PaymentResponseDto(payment.getOrderNumber(), payment.getPlan().getSlug(),
        payment.getPlan().getNameTranslations(), payment.getPeriod(), payment.getAmount(),
        payment.getCurrency(), payment.getProvider(), payment.getStatus(), payment.getCreatedAt(), payment.getPaidAt(),
        userEmail);
  }
}
