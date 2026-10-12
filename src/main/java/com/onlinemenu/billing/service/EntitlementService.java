package com.onlinemenu.billing.service;

import com.onlinemenu.billing.domain.Currency;
import com.onlinemenu.billing.domain.LimitDefinition;
import com.onlinemenu.billing.domain.LimitKind;
import com.onlinemenu.billing.domain.Plan;
import com.onlinemenu.billing.domain.Subscription;
import com.onlinemenu.billing.dto.BillingMapper;
import com.onlinemenu.billing.dto.BillingSummaryDto;
import com.onlinemenu.billing.dto.PlanLimitErrorDto;
import com.onlinemenu.billing.dto.PlanResponseDto;
import com.onlinemenu.billing.dto.SubscriptionState;
import com.onlinemenu.billing.dto.UpgradeReason;
import com.onlinemenu.billing.dto.UpgradeSuggestionDto;
import com.onlinemenu.billing.dto.UsageItemDto;
import com.onlinemenu.billing.repository.PlanRepository;
import com.onlinemenu.billing.usage.UsageCounter;
import com.onlinemenu.billing.usage.UsageStore;
import com.onlinemenu.common.constant.ExceptionMessageCodes;
import com.onlinemenu.common.constant.SettingKeys;
import com.onlinemenu.common.util.ActiveLanguageProvider;
import com.onlinemenu.common.util.LanguageContextHolder;
import com.onlinemenu.common.util.SettingProvider;
import com.onlinemenu.security.UserPrincipal;
import com.onlinemenu.user.domain.Role;
import com.onlinemenu.user.domain.User;
import com.onlinemenu.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Answers "may this account do one more X?" from its current plan. Only cafe admins are limited;
 * super admins, and anything a super admin does on someone's behalf, are not.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EntitlementService {

  private static final int TRIAL_ENDING_DAYS = 3;

  private final SubscriptionService subscriptionService;
  private final LimitDefinitionService limitDefinitionService;
  private final PlanRepository planRepository;
  private final UsageStore usageStore;
  private final List<UsageCounter> counters;
  private final UserRepository userRepository;
  private final SettingProvider settingProvider;
  private final ActiveLanguageProvider activeLanguageProvider;
  private final BillingMapper mapper;

  /** Throws when the owner already has as many as their plan allows of a COUNT limit. */
  public void checkCount(User owner, String key) {
    if (owner == null || !isEnforced(owner.getRole())) {
      return;
    }
    Optional<LimitDefinition> definition = limitDefinitionService.find(key);
    Optional<UsageCounter> counter = counter(key);
    if (definition.isEmpty() || counter.isEmpty()) {
      return;
    }
    Plan plan = planOf(owner.getId());
    long limit = limit(plan, definition.get());
    if (limit < 0) {
      return;
    }
    long used = counter.get().count(owner.getId());
    if (used >= limit) {
      throw exceeded(plan, definition.get(), limit, used);
    }
  }

  /** Counts one use of a METERED limit, throwing when the period's allowance is used up. */
  @Transactional
  public void consume(UUID userId, String key) {
    if (!tryConsume(userId, key)) {
      LimitDefinition definition = limitDefinitionService.find(key).orElseThrow();
      Plan plan = planOf(userId);
      long limit = limit(plan, definition);
      throw exceeded(plan, definition, limit, used(userId, definition));
    }
  }

  /** Counts one use of a METERED limit; false (and nothing counted) when it is used up. */
  @Transactional
  public boolean tryConsume(UUID userId, String key) {
    Optional<LimitDefinition> definition = limitDefinitionService.find(key);
    Role role = userRepository.findById(userId).map(User::getRole).orElse(null);
    if (definition.isEmpty() || role != Role.CAFE_ADMIN) {
      return true;
    }
    var periodStart = definition.get().getPeriod().start(BillingClock.today());
    if (!isEnforced(role)) {
      usageStore.increment(userId, key, periodStart);
      return true;
    }
    long limit = limit(planOf(userId), definition.get());
    return usageStore.tryIncrement(userId, key, periodStart, limit);
  }

  /**
   * Checks the path-pattern limits of an API request. Throws when a FEATURE is off or a METERED
   * allowance is used up; returns the METERED limits to count once the request succeeds.
   */
  public List<LimitDefinition> checkRequest(UUID userId, List<LimitDefinition> definitions) {
    if (definitions.isEmpty() || !isEnforced(Role.CAFE_ADMIN)) {
      return List.of();
    }
    Plan plan = planOf(userId);
    List<LimitDefinition> metered = new ArrayList<>();
    for (LimitDefinition definition : definitions) {
      long limit = limit(plan, definition);
      if (definition.getKind() == LimitKind.FEATURE && limit == 0) {
        throw exceeded(plan, definition, limit, null);
      }
      if (definition.getKind() == LimitKind.METERED) {
        long used = used(userId, definition);
        if (limit >= 0 && used >= limit) {
          throw exceeded(plan, definition, limit, used);
        }
        metered.add(definition);
      }
    }
    return metered;
  }

  @Transactional
  public void recordUse(UUID userId, List<LimitDefinition> definitions) {
    for (LimitDefinition definition : definitions) {
      usageStore.increment(userId, definition.getKey(),
          definition.getPeriod().start(BillingClock.today()));
    }
  }

  /** The owner's allowance for a limit; null when unlimited or the owner is not limited. */
  public Long limitFor(UUID userId, String key) {
    Role role = userRepository.findById(userId).map(User::getRole).orElse(null);
    if (!isEnforced(role)) {
      return null;
    }
    return limitDefinitionService.find(key)
        .map(definition -> limit(planOf(userId), definition))
        .filter(limit -> limit >= 0)
        .orElse(null);
  }

  /** One limit's usage, for places like the AI chat header; null when the user is not limited. */
  public UsageItemDto usage(UUID userId, String key) {
    Role role = userRepository.findById(userId).map(User::getRole).orElse(null);
    if (!isEnforced(role)) {
      return null;
    }
    Plan plan = planOf(userId);
    return limitDefinitionService.find(key).map(definition -> usageItem(userId, plan, definition))
        .orElse(null);
  }

  public BillingSummaryDto summary(UUID userId) {
    User user = userRepository.findById(userId).orElse(null);
    Role role = user != null ? user.getRole() : null;
    String country = user != null ? user.getCountry() : null;
    boolean enforced = isEnforced(role);
    Subscription subscription = role == Role.CAFE_ADMIN ? subscriptionService.current(userId)
        : null;
    Plan plan = subscriptionService.effectivePlan(subscription);
    SubscriptionState state = subscriptionService.state(subscription);
    Instant now = Instant.now();
    Integer daysLeft = subscription != null && subscription.isCurrent(now)
        ? BillingClock.daysLeft(now, subscription.getCurrentPeriodEnd()) : null;

    List<LimitDefinition> definitions = limitDefinitionService.definitions();
    List<UsageItemDto> usage = definitions.stream()
        .map(definition -> usageItem(userId, plan, definition)).toList();
    int promptPercent = settingProvider.getInt(SettingKeys.BILLING_UPGRADE_PROMPT_PERCENT, 80);
    UpgradeSuggestionDto suggestion = role == Role.CAFE_ADMIN && enforced
        ? suggestion(plan, subscription, state, daysLeft, definitions, usage, promptPercent)
        : null;

    return new BillingSummaryDto(mapper.toDto(plan), state,
        subscription != null ? subscription.getPeriod() : null,
        subscription != null ? subscription.getCurrentPeriodEnd() : null,
        daysLeft, usage, suggestion, promptPercent, enforced, Currency.forCountry(country), country);
  }

  private UpgradeSuggestionDto suggestion(Plan plan, Subscription subscription,
      SubscriptionState state, Integer daysLeft, List<LimitDefinition> definitions,
      List<UsageItemDto> usage, int promptPercent) {
    Map<String, LimitDefinition> byKey = definitions.stream()
        .collect(Collectors.toMap(LimitDefinition::getKey, Function.identity()));
    List<UsageItemDto> counted = usage.stream()
        .filter(item -> item.percent() != null && item.kind() != LimitKind.FEATURE)
        .sorted(Comparator.comparing(UsageItemDto::percent).reversed())
        .toList();

    for (UsageItemDto item : counted) {
      if (item.percent() >= 100) {
        PlanResponseDto upgrade = upgradeFor(plan, byKey.get(item.key()));
        if (upgrade != null) {
          return new UpgradeSuggestionDto(UpgradeReason.LIMIT_REACHED, item.key(), upgrade);
        }
      }
    }
    if (state == SubscriptionState.EXPIRED || state == SubscriptionState.FREE) {
      PlanResponseDto renew = renewalPlan(subscription);
      if (renew != null) {
        return new UpgradeSuggestionDto(UpgradeReason.EXPIRED, null, renew);
      }
    }
    if (state == SubscriptionState.TRIAL && daysLeft != null && daysLeft <= TRIAL_ENDING_DAYS) {
      PlanResponseDto renew = renewalPlan(subscription);
      if (renew != null) {
        return new UpgradeSuggestionDto(UpgradeReason.TRIAL_ENDING, null, renew);
      }
    }
    for (UsageItemDto item : counted) {
      if (item.percent() >= promptPercent) {
        PlanResponseDto upgrade = upgradeFor(plan, byKey.get(item.key()));
        if (upgrade != null) {
          return new UpgradeSuggestionDto(UpgradeReason.LIMIT_NEAR, item.key(), upgrade);
        }
      }
    }
    return null;
  }

  /** The plan to buy after a trial or an expired period: the same one if it is on sale. */
  private PlanResponseDto renewalPlan(Subscription subscription) {
    List<Plan> onSale = plansOnSale();
    if (subscription != null && onSale.stream()
        .anyMatch(plan -> plan.getId().equals(subscription.getPlan().getId()))) {
      return mapper.toDto(subscription.getPlan());
    }
    return onSale.stream().filter(Plan::isRecommended).findFirst()
        .or(() -> onSale.stream().findFirst())
        .map(mapper::toDto).orElse(null);
  }

  /** The cheapest plan on sale that allows more of this limit than the current one. */
  private PlanResponseDto upgradeFor(Plan current, LimitDefinition definition) {
    if (definition == null) {
      return null;
    }
    long currentLimit = limit(current, definition);
    if (currentLimit < 0) {
      return null;
    }
    return plansOnSale().stream()
        .filter(plan -> !plan.getSlug().equals(current.getSlug()))
        .filter(plan -> {
          long limit = limit(plan, definition);
          return definition.getKind() == LimitKind.FEATURE ? limit != 0
              : limit < 0 || limit > currentLimit;
        })
        .findFirst()
        .map(mapper::toDto)
        .orElse(null);
  }

  private List<Plan> plansOnSale() {
    return planRepository.findAllByActiveTrueOrderBySortOrderAscMonthlyPriceAsc().stream()
        .filter(plan -> plan.getMonthlyPrice() > 0 || plan.getYearlyPrice() > 0)
        .sorted(Comparator.comparingLong(Plan::getMonthlyPrice))
        .toList();
  }

  private UsageItemDto usageItem(UUID userId, Plan plan, LimitDefinition definition) {
    long limit = limit(plan, definition);
    Long used = definition.getKind() == LimitKind.FEATURE ? null : used(userId, definition);
    Integer percent = null;
    if (used != null && limit >= 0) {
      percent = limit == 0 ? 100 : (int) Math.min(100, used * 100 / limit);
    }
    return new UsageItemDto(definition.getKey(), definition.getNameTranslations(),
        definition.getKind(), definition.getPeriod(), limit < 0 ? null : limit, used, percent,
        limit != 0);
  }

  private Long used(UUID userId, LimitDefinition definition) {
    return switch (definition.getKind()) {
      case COUNT -> counter(definition.getKey()).map(counter -> counter.count(userId)).orElse(null);
      case METERED -> usageStore.used(userId, definition.getKey(),
          definition.getPeriod().start(BillingClock.today()));
      case FEATURE -> null;
    };
  }

  private PlanLimitExceededException exceeded(Plan plan, LimitDefinition definition, long limit,
      Long used) {
    PlanLimitErrorDto details = new PlanLimitErrorDto(definition.getKey(),
        definition.getNameTranslations(), definition.getKind(), limit < 0 ? null : limit, used,
        plan.getSlug(), upgradeFor(plan, definition));
    String name = localizedName(definition);
    if (definition.getKind() == LimitKind.FEATURE) {
      return new PlanLimitExceededException(ExceptionMessageCodes.BILLING_FEATURE_UNAVAILABLE,
          details, name);
    }
    return new PlanLimitExceededException(ExceptionMessageCodes.BILLING_LIMIT_REACHED, details,
        name, used == null ? String.valueOf(limit) : used + "/" + limit);
  }

  private String localizedName(LimitDefinition definition) {
    Map<String, String> names = definition.getNameTranslations();
    String name = names.get(LanguageContextHolder.get());
    if (name == null) {
      name = names.get(activeLanguageProvider.getDefaultLanguageCode());
    }
    return name != null ? name : names.values().stream().findFirst().orElse(definition.getKey());
  }

  private Plan planOf(UUID userId) {
    return subscriptionService.effectivePlan(subscriptionService.current(userId));
  }

  static long limit(Plan plan, LimitDefinition definition) {
    Long value = plan.getEntitlements().get(definition.getKey());
    return value != null ? value : definition.getDefaultValue();
  }

  private Optional<UsageCounter> counter(String key) {
    return counters.stream().filter(counter -> counter.key().equals(key)).findFirst();
  }

  private boolean isEnforced(Role ownerRole) {
    return ownerRole == Role.CAFE_ADMIN && !actingAsSuperAdmin()
        && settingProvider.getBoolean(SettingKeys.BILLING_ENABLED, true);
  }

  private static boolean actingAsSuperAdmin() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal
        && Role.SUPER_ADMIN.name().equals(principal.getRole());
  }
}
