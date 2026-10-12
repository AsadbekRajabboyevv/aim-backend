package com.onlinemenu.billing.payment;

import com.onlinemenu.billing.domain.Currency;
import com.onlinemenu.billing.domain.Payment;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.List;

/**
 * One payment system. To add a new one, implement this as a Spring bean, put its keys under
 * {@code app.payment.providers.<code>} and register
 * {@code <callback-base-url>/api/v1/public/payments/<code>} in its merchant cabinet. Checkout,
 * the admin toggle and the callback route then work without other changes.
 *
 * <p>Implementations only translate their protocol: they find the order with
 * {@link com.onlinemenu.billing.service.PaymentService#lockByOrder} and change it only through
 * {@code markPaid} / {@code cancel}, which also move the subscription.
 */
public interface PaymentProvider {

  /** Lower-case id used in URLs and settings, e.g. "payme". */
  String code();

  String displayName();

  /** Whether the server has the keys this provider needs. */
  boolean isConfigured();

  /** Environment variables the keys are read from, shown to the super admin. */
  List<String> envVariables();

  /** Whether this system can charge in the currency. The Uzbek systems take so'm only. */
  default boolean supports(Currency currency) {
    return currency == Currency.UZS;
  }

  /** Where to send the user to pay for the order; they come back to {@code returnUrl}. */
  String checkoutUrl(Payment payment, String returnUrl);

  /**
   * Handles a request from the payment system to {@code /api/v1/public/payments/<code>/<path>}.
   *
   * @param path the part after the provider code, e.g. "/check" or "" (never null)
   */
  ResponseEntity<?> handleCallback(String path, HttpServletRequest request) throws IOException;
}
