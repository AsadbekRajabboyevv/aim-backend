package com.onlinemenu.billing.service;

import com.onlinemenu.billing.domain.Currency;
import com.onlinemenu.billing.domain.BillingPeriod;
import com.onlinemenu.billing.domain.Payment;
import com.onlinemenu.billing.domain.Plan;
import com.onlinemenu.billing.domain.Subscription;
import com.onlinemenu.billing.domain.SubscriptionStatus;
import com.onlinemenu.billing.dto.QuoteDto;
import com.onlinemenu.billing.dto.SubscriptionAdminDto;
import com.onlinemenu.billing.dto.SubscriptionAssignRequestDto;
import com.onlinemenu.billing.dto.SubscriptionState;
import com.onlinemenu.billing.repository.PlanRepository;
import com.onlinemenu.billing.repository.SubscriptionRepository;
import com.onlinemenu.common.constant.ExceptionMessageCodes;
import com.onlinemenu.common.constant.SettingKeys;
import com.onlinemenu.common.exception.BusinessException;
import com.onlinemenu.common.exception.ResourceNotFoundException;
import com.onlinemenu.common.util.SettingProvider;
import com.onlinemenu.user.domain.Role;
import com.onlinemenu.user.domain.User;
import com.onlinemenu.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionService {

  private final SubscriptionRepository subscriptionRepository;
  private final PlanRepository planRepository;
  private final UserRepository userRepository;
  private final TrialStarter trialStarter;
  private final SettingProvider settingProvider;
  private final UserCurrency userCurrency;

  /** The user's subscription row, starting the free trial for a cafe admin who has none. */
  public Subscription current(UUID userId) {
    var existing = subscriptionRepository.findByUserId(userId);
    if (existing.isPresent()) {
      return existing.get();
    }
    boolean cafeAdmin = userRepository.findById(userId).map(User::getRole)
        .filter(role -> role == Role.CAFE_ADMIN).isPresent();
    if (!cafeAdmin) {
      return null;
    }
    try {
      return trialStarter.start(userId);
    } catch (DataIntegrityViolationException ex) {
      // Another request started the trial at the same moment.
      return subscriptionRepository.findByUserId(userId).orElse(null);
    }
  }

  public Plan effectivePlan(Subscription subscription) {
    if (subscription != null && subscription.isCurrent(Instant.now())) {
      return subscription.getPlan();
    }
    return fallbackPlan();
  }

  /** The plan for accounts without a paid or trial period; an empty plan if it is missing. */
  public Plan fallbackPlan() {
    return planRepository.findBySlug(
            settingProvider.getString(SettingKeys.BILLING_FALLBACK_PLAN, ""))
        .orElseGet(() -> {
          log.warn("Billing fallback plan is missing; limits fall back to their default values");
          Plan empty = Plan.builder().nameTranslations(Map.of()).build();
          empty.setSlug("");
          return empty;
        });
  }

  public SubscriptionState state(Subscription subscription) {
    if (subscription == null) {
      return SubscriptionState.FREE;
    }
    if (!subscription.isCurrent(Instant.now())) {
      return SubscriptionState.EXPIRED;
    }
    return subscription.getStatus() == SubscriptionStatus.TRIAL ? SubscriptionState.TRIAL
        : SubscriptionState.ACTIVE;
  }

  public QuoteDto quote(UUID userId, Plan plan, BillingPeriod period) {
    Currency currency = userCurrency.of(userId);
    return quote(current(userId), plan, period, plan.price(period, currency), currency, Instant.now())
        .dto(plan, period, currency);
  }

  /**
   * Called in the transaction that marks the payment paid. The same plan and period extends the
   * current one; another plan starts now and gets the unused paid time converted into its days.
   */
  @Transactional
  public void applyPayment(Payment payment) {
    Instant now = Instant.now();
    Subscription subscription = subscriptionRepository.findByUserIdForUpdate(payment.getUserId())
        .orElse(null);
    Quote quote = quote(subscription, payment.getPlan(), payment.getPeriod(),
        payment.getAmount(), payment.getCurrency(), now);
    if (subscription == null) {
      subscription = Subscription.builder().userId(payment.getUserId()).build();
    }
    subscription.setPlan(payment.getPlan());
    subscription.setStatus(SubscriptionStatus.ACTIVE);
    subscription.setPeriod(payment.getPeriod());
    subscription.setCurrentPeriodStart(now);
    subscription.setCurrentPeriodEnd(quote.endsAt());
    subscription.setPaidAmount(payment.getAmount() + quote.carriedValue());
    subscription.setPaidCurrency(payment.getCurrency());
    subscriptionRepository.save(subscription);
    log.info("Order {} paid: user {} is on plan {} until {}", payment.getOrderNumber(),
        payment.getUserId(), payment.getPlan().getSlug(), quote.endsAt());
  }

  public Page<SubscriptionAdminDto> search(String search, Pageable pageable) {
    Page<User> users = subscriptionRepository.searchCafeAdmins(search == null ? "" : search.trim(),
        pageable);
    Map<UUID, Subscription> subscriptions = subscriptionRepository
        .findAllByUserIdIn(users.map(User::getId).toList()).stream()
        .collect(Collectors.toMap(Subscription::getUserId, Function.identity()));
    return users.map(user -> toAdminDto(user, subscriptions.get(user.getId())));
  }

  @Transactional
  public SubscriptionAdminDto assign(String userSlug, SubscriptionAssignRequestDto dto) {
    User user = userRepository.findBySlug(userSlug)
        .orElseThrow(() -> new ResourceNotFoundException(ExceptionMessageCodes.USER_NOT_FOUND,
            userSlug));
    if (user.getRole() != Role.CAFE_ADMIN) {
      throw new BusinessException(ExceptionMessageCodes.CAFE_OWNER_INVALID, HttpStatus.BAD_REQUEST);
    }
    Plan plan = planRepository.findBySlug(dto.planSlug())
        .orElseThrow(() -> new ResourceNotFoundException(
            ExceptionMessageCodes.BILLING_PLAN_NOT_FOUND, dto.planSlug()));
    Subscription subscription = subscriptionRepository.findByUserIdForUpdate(user.getId())
        .orElseGet(() -> Subscription.builder().userId(user.getId()).build());
    subscription.setPlan(plan);
    subscription.setStatus(dto.period() == null ? SubscriptionStatus.TRIAL
        : SubscriptionStatus.ACTIVE);
    subscription.setPeriod(dto.period());
    subscription.setCurrentPeriodStart(Instant.now());
    subscription.setCurrentPeriodEnd(dto.currentPeriodEnd());
    subscription.setPaidAmount(0);
    subscription.setPaidCurrency(Currency.forCountry(user.getCountry()));
    return toAdminDto(user, subscriptionRepository.save(subscription));
  }

  private SubscriptionAdminDto toAdminDto(User user, Subscription subscription) {
    Plan plan = subscription != null ? subscription.getPlan() : null;
    return new SubscriptionAdminDto(user.getSlug(), user.getFullName(), user.getEmail(),
        plan != null ? plan.getSlug() : null, plan != null ? plan.getNameTranslations() : Map.of(),
        state(subscription), subscription != null ? subscription.getPeriod() : null,
        subscription != null ? subscription.getCurrentPeriodEnd() : null);
  }

  private Quote quote(Subscription current, Plan plan, BillingPeriod period, long price,
      Currency currency, Instant now) {
    boolean paidAndRunning = current != null && current.getStatus() == SubscriptionStatus.ACTIVE
        && current.isCurrent(now);
    long carried = paidAndRunning ? unusedValue(current, currency, now) : 0;
    boolean renewal = paidAndRunning && current.getPlan().getId().equals(plan.getId())
        && current.getPeriod() == period;
    if (renewal) {
      Instant end = BillingClock.plus(current.getCurrentPeriodEnd(), period);
      return new Quote(price, true, 0, now, end, carried);
    }
    Instant base = BillingClock.plus(now, period);
    long bonusSeconds = 0;
    if (carried > 0 && price > 0) {
      long periodSeconds = Duration.between(now, base).getSeconds();
      bonusSeconds = BigDecimal.valueOf(carried).multiply(BigDecimal.valueOf(periodSeconds))
          .divide(BigDecimal.valueOf(price), 0, RoundingMode.DOWN).longValue();
    }
    return new Quote(price, false, (int) (bonusSeconds / 86_400), now,
        base.plusSeconds(bonusSeconds), carried);
  }

  /**
   * What the not yet used part of the current paid period is worth, in {@code currency}. Paid in
   * another currency (the user moved country), it is valued at the current plan's price in the new one.
   */
  private static long unusedValue(Subscription subscription, Currency currency, Instant now) {
    long total = Duration.between(subscription.getCurrentPeriodStart(),
        subscription.getCurrentPeriodEnd()).getSeconds();
    long remaining = Duration.between(now, subscription.getCurrentPeriodEnd()).getSeconds();
    long paid = subscription.getPaidCurrency() == currency ? subscription.getPaidAmount()
        : subscription.getPeriod() == null ? 0
        : subscription.getPlan().price(subscription.getPeriod(), currency);
    if (total <= 0 || remaining <= 0 || paid <= 0) {
      return 0;
    }
    return BigDecimal.valueOf(paid).multiply(BigDecimal.valueOf(remaining))
        .divide(BigDecimal.valueOf(total), 0, RoundingMode.DOWN).longValue();
  }

  private record Quote(long amount, boolean renewal, int bonusDays, Instant startsAt,
                       Instant endsAt, long carriedValue) {

    QuoteDto dto(Plan plan, BillingPeriod period, Currency currency) {
      return new QuoteDto(plan.getSlug(), period, amount, currency, renewal, bonusDays, startsAt, endsAt);
    }
  }
}
