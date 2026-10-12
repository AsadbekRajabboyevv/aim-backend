package com.onlinemenu.billing.payment.sandbox;

import com.onlinemenu.billing.domain.Currency;
import com.onlinemenu.billing.domain.Payment;
import com.onlinemenu.billing.domain.PaymentStatus;
import com.onlinemenu.billing.payment.PaymentProperties;
import com.onlinemenu.billing.payment.PaymentProvider;
import com.onlinemenu.billing.payment.PaymentProviderRegistry;
import com.onlinemenu.billing.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * A fake payment system for development: the checkout link marks the order paid at once and
 * returns to the panel. Off unless {@code PAYMENT_SANDBOX_ENABLED=true}; never turn it on in
 * production.
 */
@Component
@RequiredArgsConstructor
public class SandboxProvider implements PaymentProvider {

  static final String CODE = "sandbox";

  private final PaymentProperties properties;
  private final PaymentService paymentService;

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public String displayName() {
    return "Test to'lov (sandbox)";
  }

  @Override
  public boolean isConfigured() {
    return "true".equalsIgnoreCase(properties.value(CODE, "enabled"));
  }

  @Override
  public List<String> envVariables() {
    return List.of("PAYMENT_SANDBOX_ENABLED");
  }

  @Override
  public boolean supports(Currency currency) {
    return true;
  }

  @Override
  public String checkoutUrl(Payment payment, String returnUrl) {
    return properties.callbackBaseUrl() + PaymentProviderRegistry.CALLBACK_PATH + CODE
        + "/pay?order=" + payment.getOrderNumber();
  }

  @Override
  @Transactional
  public ResponseEntity<?> handleCallback(String path, HttpServletRequest request) {
    if (!isConfigured() || !"/pay".equals(path)) {
      return ResponseEntity.notFound().build();
    }
    var payment = paymentService.lockByOrder(request.getParameter("order"))
        .filter(order -> CODE.equals(order.getProvider()));
    if (payment.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    if (payment.get().getStatus() == PaymentStatus.PENDING) {
      paymentService.markPaid(payment.get());
    }
    String returnUrl = properties.returnUrl();
    return ResponseEntity.status(HttpStatus.FOUND)
        .header(HttpHeaders.LOCATION, returnUrl + (returnUrl.contains("?") ? "&" : "?")
            + "order=" + payment.get().getOrderNumber())
        .build();
  }
}
