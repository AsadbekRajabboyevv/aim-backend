package com.onlinemenu.billing.service;

import com.onlinemenu.billing.dto.BillingSettingsDto;
import com.onlinemenu.billing.dto.BillingSettingsRequestDto;
import com.onlinemenu.billing.dto.ProviderSettingsDto;
import com.onlinemenu.billing.payment.PaymentProvider;
import com.onlinemenu.billing.payment.PaymentProviderRegistry;
import com.onlinemenu.common.constant.SettingKeys;
import com.onlinemenu.common.exception.ResourceNotFoundException;
import com.onlinemenu.setting.domain.SettingType;
import com.onlinemenu.setting.dto.SettingCreateRequestDto;
import com.onlinemenu.setting.dto.SettingRequestDto;
import com.onlinemenu.setting.service.SettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * The billing part of the platform settings. Values live in app_setting, so they are also visible
 * on the general settings page.
 */
@Service
@RequiredArgsConstructor
public class BillingSettingsService {

  private final SettingService settingService;
  private final PaymentProviderRegistry providerRegistry;
  private final PlanService planService;

  public BillingSettingsDto get() {
    return new BillingSettingsDto(
        settingService.getBoolean(SettingKeys.BILLING_ENABLED, true),
        settingService.getInt(SettingKeys.BILLING_TRIAL_DAYS, 14),
        settingService.getString(SettingKeys.BILLING_TRIAL_PLAN, ""),
        settingService.getString(SettingKeys.BILLING_FALLBACK_PLAN, ""),
        settingService.getInt(SettingKeys.BILLING_UPGRADE_PROMPT_PERCENT, 80),
        providerRegistry.all().stream().map(this::toDto).toList());
  }

  public BillingSettingsDto update(BillingSettingsRequestDto dto) {
    String trialPlan = planService.findOrThrow(dto.trialPlan().trim()).getSlug();
    String fallbackPlan = planService.findOrThrow(dto.fallbackPlan().trim()).getSlug();
    upsert(SettingKeys.BILLING_ENABLED, String.valueOf(dto.enabled()), SettingType.BOOLEAN,
        "Plan limits on/off. Off: nobody is limited");
    upsert(SettingKeys.BILLING_TRIAL_DAYS, String.valueOf(dto.trialDays()), SettingType.INTEGER,
        "Free trial length for new cafe admins (0 = no trial)");
    upsert(SettingKeys.BILLING_TRIAL_PLAN, trialPlan, SettingType.STRING,
        "Slug of the plan given during the free trial");
    upsert(SettingKeys.BILLING_FALLBACK_PLAN, fallbackPlan, SettingType.STRING,
        "Slug of the plan that applies when there is no paid or trial period");
    upsert(SettingKeys.BILLING_UPGRADE_PROMPT_PERCENT, String.valueOf(dto.promptPercent()),
        SettingType.INTEGER, "Usage percent of a limit at which the panel suggests a bigger plan");
    Map<String, Boolean> providers = dto.providers() == null ? Map.of() : dto.providers();
    for (PaymentProvider provider : providerRegistry.all()) {
      Boolean enabled = providers.get(provider.code());
      if (enabled != null) {
        upsert(SettingKeys.paymentEnabled(provider.code()), String.valueOf(enabled),
            SettingType.BOOLEAN, "Offer " + provider.displayName() + " at checkout");
      }
    }
    return get();
  }

  private ProviderSettingsDto toDto(PaymentProvider provider) {
    return new ProviderSettingsDto(provider.code(), provider.displayName(),
        provider.isConfigured(), providerRegistry.isEnabled(provider),
        providerRegistry.callbackUrl(provider), provider.envVariables());
  }

  private void upsert(String key, String value, SettingType type, String description) {
    try {
      settingService.get(key);
      settingService.update(key, new SettingRequestDto(value, null, null));
    } catch (ResourceNotFoundException ex) {
      settingService.create(new SettingCreateRequestDto(key, value, type, description));
    }
  }
}
