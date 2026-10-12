package com.onlinemenu.common.constant;

import java.util.Set;

public final class SettingKeys {

  private SettingKeys() {
  }

  public static final String MENU_PUBLIC_BASE_URL = "menu.public_base_url";

  public static final String AI_ENABLED = "ai.enabled";

  public static final String AI_PROVIDER = "ai.provider";

  public static final String BILLING_ENABLED = "billing.enabled";

  public static final String BILLING_TRIAL_DAYS = "billing.trial_days";

  public static final String BILLING_TRIAL_PLAN = "billing.trial_plan";

  public static final String BILLING_FALLBACK_PLAN = "billing.fallback_plan";

  public static final String BILLING_UPGRADE_PROMPT_PERCENT = "billing.upgrade_prompt_percent";

  public static String aiModel(String providerId) {
    return "ai." + providerId + ".model";
  }

  public static String aiTitleModel(String providerId) {
    return "ai." + providerId + ".title_model";
  }

  public static String paymentEnabled(String providerCode) {
    return "payment." + providerCode + ".enabled";
  }

  public static final Set<String> PROTECTED = Set.of(MENU_PUBLIC_BASE_URL,
      AI_ENABLED, AI_PROVIDER,
      aiModel("openai"), aiTitleModel("openai"), aiModel("gemini"), aiTitleModel("gemini"),
      BILLING_ENABLED, BILLING_TRIAL_DAYS, BILLING_TRIAL_PLAN, BILLING_FALLBACK_PLAN,
      BILLING_UPGRADE_PROMPT_PERCENT,
      paymentEnabled("payme"), paymentEnabled("click"), paymentEnabled("uzum"),
      paymentEnabled("lemonsqueezy"));
}
