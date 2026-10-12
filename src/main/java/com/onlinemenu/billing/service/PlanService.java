package com.onlinemenu.billing.service;

import com.onlinemenu.billing.domain.Currency;
import com.onlinemenu.billing.domain.Plan;
import com.onlinemenu.billing.dto.BillingMapper;
import com.onlinemenu.billing.dto.PlanCatalogDto;
import com.onlinemenu.billing.dto.PlanRequestDto;
import com.onlinemenu.billing.dto.PlanResponseDto;
import com.onlinemenu.billing.repository.PaymentRepository;
import com.onlinemenu.billing.repository.PlanRepository;
import com.onlinemenu.billing.repository.SubscriptionRepository;
import com.onlinemenu.common.constant.ExceptionMessageCodes;
import com.onlinemenu.common.constant.SettingKeys;
import com.onlinemenu.common.exception.BusinessException;
import com.onlinemenu.common.exception.ResourceNotFoundException;
import com.onlinemenu.common.util.ActiveLanguageProvider;
import com.onlinemenu.common.util.SettingProvider;
import com.onlinemenu.common.util.SlugUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanService {

  private final PlanRepository planRepository;
  private final SubscriptionRepository subscriptionRepository;
  private final PaymentRepository paymentRepository;
  private final LimitDefinitionService limitDefinitionService;
  private final ActiveLanguageProvider activeLanguageProvider;
  private final SettingProvider settingProvider;
  private final BillingMapper mapper;

  public List<PlanResponseDto> get() {
    return planRepository.findAllByOrderBySortOrderAscMonthlyPriceAsc().stream()
        .map(mapper::toDto).toList();
  }

  /** What the pricing pages show: plans that can be bought and every limit. */
  public PlanCatalogDto catalog(Currency currency) {
    return new PlanCatalogDto(
        planRepository.findAllByActiveTrueOrderBySortOrderAscMonthlyPriceAsc().stream()
            .map(mapper::toDto).toList(),
        limitDefinitionService.get(), currency);
  }

  public Plan findOrThrow(String slug) {
    return planRepository.findBySlug(slug)
        .orElseThrow(
            () -> new ResourceNotFoundException(ExceptionMessageCodes.BILLING_PLAN_NOT_FOUND, slug));
  }

  @Transactional
  public PlanResponseDto create(PlanRequestDto dto) {
    Plan plan = new Plan();
    apply(plan, dto);
    plan.setSlug(SlugUtils.generateUniqueSlug(primaryName(plan.getNameTranslations()),
        planRepository::existsBySlug));
    return mapper.toDto(planRepository.save(plan));
  }

  @Transactional
  public PlanResponseDto update(String slug, PlanRequestDto dto) {
    Plan plan = findOrThrow(slug);
    apply(plan, dto);
    return mapper.toDto(plan);
  }

  @Transactional
  public void delete(String slug) {
    Plan plan = findOrThrow(slug);
    boolean referencedBySettings =
        slug.equals(settingProvider.getString(SettingKeys.BILLING_TRIAL_PLAN, ""))
            || slug.equals(settingProvider.getString(SettingKeys.BILLING_FALLBACK_PLAN, ""));
    if (referencedBySettings || subscriptionRepository.existsByPlanId(plan.getId())
        || paymentRepository.existsByPlanId(plan.getId())) {
      throw new BusinessException(ExceptionMessageCodes.BILLING_PLAN_IN_USE, HttpStatus.CONFLICT);
    }
    planRepository.delete(plan);
  }

  private static long orZero(Long value) {
    return value == null ? 0 : value;
  }

  private void apply(Plan plan, PlanRequestDto dto) {
    plan.setNameTranslations(clean(dto.nameTranslations()));
    plan.setDescriptionTranslations(clean(dto.descriptionTranslations()));
    plan.setMonthlyPrice(dto.monthlyPrice());
    plan.setYearlyPrice(dto.yearlyPrice());
    plan.setMonthlyPriceUsd(orZero(dto.monthlyPriceUsd()));
    plan.setYearlyPriceUsd(orZero(dto.yearlyPriceUsd()));
    plan.setMonthlyPriceEur(orZero(dto.monthlyPriceEur()));
    plan.setYearlyPriceEur(orZero(dto.yearlyPriceEur()));
    plan.setEntitlements(limitDefinitionService.sanitize(dto.entitlements()));
    plan.setSortOrder(dto.sortOrder() == null ? plan.getSortOrder() : dto.sortOrder());
    plan.setRecommended(dto.recommended());
    plan.setActive(dto.active());
    if (plan.isRecommended()) {
      planRepository.findAll().stream()
          .filter(other -> other != plan && !other.getId().equals(plan.getId()))
          .forEach(other -> other.setRecommended(false));
    }
  }

  private String primaryName(Map<String, String> names) {
    String name = names.get(activeLanguageProvider.getDefaultLanguageCode());
    return name != null ? name : names.values().stream().findFirst().orElse("plan");
  }

  private static Map<String, String> clean(Map<String, String> translations) {
    Map<String, String> result = new HashMap<>();
    if (translations != null) {
      translations.forEach((code, text) -> {
        if (code != null && text != null && !text.isBlank()) {
          result.put(code, text.trim());
        }
      });
    }
    return result;
  }
}
