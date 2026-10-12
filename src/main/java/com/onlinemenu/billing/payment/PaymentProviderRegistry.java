package com.onlinemenu.billing.payment;

import com.onlinemenu.billing.domain.Payment;
import com.onlinemenu.common.constant.ExceptionMessageCodes;
import com.onlinemenu.common.constant.SettingKeys;
import com.onlinemenu.common.exception.BusinessException;
import com.onlinemenu.common.util.SettingProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PaymentProviderRegistry {

  public static final String CALLBACK_PATH = "/api/v1/public/payments/";

  private final List<PaymentProvider> providers;
  private final SettingProvider settingProvider;
  private final PaymentProperties properties;

  public List<PaymentProvider> all() {
    return providers.stream().sorted(Comparator.comparing(PaymentProvider::code)).toList();
  }

  public Optional<PaymentProvider> find(String code) {
    return providers.stream().filter(provider -> provider.code().equalsIgnoreCase(code))
        .findFirst();
  }

  /** Turned on by the super admin; a provider without a setting row is on. */
  public boolean isEnabled(PaymentProvider provider) {
    return settingProvider.getBoolean(SettingKeys.paymentEnabled(provider.code()), true);
  }

  /** Providers users can pay with right now. */
  public List<PaymentProvider> available() {
    return all().stream().filter(PaymentProvider::isConfigured).filter(this::isEnabled).toList();
  }

  public PaymentProvider availableOrThrow(String code) {
    return find(code).filter(PaymentProvider::isConfigured).filter(this::isEnabled)
        .orElseThrow(() -> new BusinessException(
            ExceptionMessageCodes.BILLING_PROVIDER_UNAVAILABLE, HttpStatus.BAD_REQUEST));
  }

  public String callbackUrl(PaymentProvider provider) {
    return properties.callbackBaseUrl() + CALLBACK_PATH + provider.code();
  }

  public String returnUrl(Payment payment) {
    String base = properties.returnUrl();
    return base + (base.contains("?") ? "&" : "?") + "order=" + payment.getOrderNumber();
  }
}
