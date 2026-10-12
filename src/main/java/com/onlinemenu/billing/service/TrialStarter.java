package com.onlinemenu.billing.service;

import com.onlinemenu.billing.domain.Subscription;
import com.onlinemenu.billing.domain.SubscriptionStatus;
import com.onlinemenu.billing.repository.PlanRepository;
import com.onlinemenu.billing.repository.SubscriptionRepository;
import com.onlinemenu.common.constant.SettingKeys;
import com.onlinemenu.common.util.SettingProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Gives a cafe admin the free trial the first time billing looks at them, whichever way the
 * account was created (sign-up, Google, super admin). Runs in its own transaction so read-only
 * callers can use it.
 */
@Component
@RequiredArgsConstructor
class TrialStarter {

  private final SubscriptionRepository subscriptionRepository;
  private final PlanRepository planRepository;
  private final SettingProvider settingProvider;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Subscription start(UUID userId) {
    int days = settingProvider.getInt(SettingKeys.BILLING_TRIAL_DAYS, 14);
    if (days <= 0) {
      return null;
    }
    return planRepository.findBySlug(settingProvider.getString(SettingKeys.BILLING_TRIAL_PLAN, ""))
        .map(plan -> {
          Instant now = Instant.now();
          return subscriptionRepository.saveAndFlush(Subscription.builder()
              .userId(userId)
              .plan(plan)
              .status(SubscriptionStatus.TRIAL)
              .currentPeriodStart(now)
              .currentPeriodEnd(now.plus(Duration.ofDays(days)))
              .paidAmount(0)
              .build());
        })
        .orElse(null);
  }
}
