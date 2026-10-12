package com.onlinemenu.billing.payment;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * {@code app.payment.*}. Each provider reads its own keys from {@code providers.<code>}, so adding
 * a provider needs no change here.
 *
 * @param returnUrl       the panel page users return to after paying (billing page)
 * @param callbackBaseUrl public address of this backend, used to show callback URLs
 */
@ConfigurationProperties(prefix = "app.payment")
public record PaymentProperties(Map<String, Map<String, String>> providers, String returnUrl,
                                String callbackBaseUrl) {

  public PaymentProperties {
    providers = providers == null ? new HashMap<>() : providers;
    returnUrl = StringUtils.hasText(returnUrl) ? returnUrl : "http://localhost:4200/billing";
    callbackBaseUrl = StringUtils.hasText(callbackBaseUrl) ? callbackBaseUrl.replaceAll("/+$", "")
        : "http://localhost:8080";
  }

  /** A provider key, or null when it is missing or blank. */
  public String value(String provider, String key) {
    String value = providers.getOrDefault(provider, Map.of()).get(key);
    return StringUtils.hasText(value) ? value.trim() : null;
  }

  public String value(String provider, String key, String defaultValue) {
    String value = value(provider, key);
    return value != null ? value : defaultValue;
  }
}
