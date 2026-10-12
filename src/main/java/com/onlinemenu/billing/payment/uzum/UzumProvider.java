package com.onlinemenu.billing.payment.uzum;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinemenu.billing.domain.Payment;
import com.onlinemenu.billing.domain.PaymentStatus;
import com.onlinemenu.billing.payment.CallbackAuth;
import com.onlinemenu.billing.payment.PaymentProperties;
import com.onlinemenu.billing.payment.PaymentProvider;
import com.onlinemenu.billing.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Uzum Bank Merchant API: Uzum calls {@code /check}, {@code /create}, {@code /confirm},
 * {@code /reverse} and {@code /status} under the callback URL with HTTP Basic auth. Amounts are in
 * tiyin. Error codes and the checkout link come from the merchant documentation Uzum gives on
 * connection; they are kept in {@link UzumError} and {@code app.payment.providers.uzum.*} so they
 * can be adjusted without touching the flow.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UzumProvider implements PaymentProvider {

  static final String CODE = "uzum";

  private final PaymentProperties properties;
  private final PaymentService paymentService;
  private final ObjectMapper objectMapper;

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public String displayName() {
    return "Uzum Bank";
  }

  @Override
  public boolean isConfigured() {
    return serviceId() != null && properties.value(CODE, "login") != null
        && properties.value(CODE, "password") != null;
  }

  @Override
  public List<String> envVariables() {
    return List.of("UZUM_SERVICE_ID", "UZUM_LOGIN", "UZUM_PASSWORD", "UZUM_CHECKOUT_URL");
  }

  @Override
  public String checkoutUrl(Payment payment, String returnUrl) {
    String template = properties.value(CODE, "checkout-url",
        "https://www.uzumbank.uz/open-service?serviceId={serviceId}&order_id={orderId}&amount={amountTiyin}&redirectUrl={returnUrl}");
    return template
        .replace("{serviceId}", serviceId())
        .replace("{orderId}", String.valueOf(payment.getOrderNumber()))
        .replace("{amountTiyin}", String.valueOf(tiyin(payment)))
        .replace("{amount}", String.valueOf(payment.getAmount()))
        .replace("{returnUrl}", URLEncoder.encode(returnUrl, StandardCharsets.UTF_8));
  }

  @Override
  @Transactional
  public ResponseEntity<?> handleCallback(String path, HttpServletRequest request)
      throws IOException {
    if (!CallbackAuth.basicMatches(request, properties.value(CODE, "login"),
        properties.value(CODE, "password"))) {
      return error(null, UzumError.ACCESS_DENIED);
    }
    JsonNode body;
    try {
      body = objectMapper.readTree(request.getInputStream());
    } catch (IOException ex) {
      return error(null, UzumError.JSON_PARSING);
    }
    if (body == null || !serviceId().equals(body.path("serviceId").asText())) {
      return error(body, UzumError.SERVICE_INVALID);
    }
    return switch (path) {
      case "/check" -> check(body);
      case "/create" -> create(body);
      case "/confirm" -> confirm(body);
      case "/reverse" -> reverse(body);
      case "/status" -> status(body);
      default -> error(body, UzumError.INVALID_OPERATION);
    };
  }

  private ResponseEntity<?> check(JsonNode body) {
    Optional<Payment> payment = order(body);
    if (payment.isEmpty()) {
      return error(body, UzumError.ORDER_NOT_FOUND);
    }
    if (payment.get().getStatus() != PaymentStatus.PENDING) {
      return error(body, UzumError.ALREADY_PAID);
    }
    Map<String, Object> response = base(body);
    response.put("status", "OK");
    response.put("data", data(payment.get()));
    return ResponseEntity.ok(response);
  }

  private ResponseEntity<?> create(JsonNode body) {
    String transactionId = body.path("transId").asText();
    Optional<Payment> existing = paymentService.lockByTransaction(CODE, transactionId);
    if (existing.isPresent()) {
      return transaction(body, existing.get());
    }
    Optional<Payment> found = order(body);
    if (found.isEmpty()) {
      return error(body, UzumError.ORDER_NOT_FOUND);
    }
    Payment payment = found.get();
    if (payment.getStatus() != PaymentStatus.PENDING || payment.getProviderTransactionId() != null) {
      return error(body, UzumError.ALREADY_PAID);
    }
    if (body.path("amount").asLong(-1) != tiyin(payment)) {
      return error(body, UzumError.WRONG_AMOUNT);
    }
    payment.setProviderTransactionId(transactionId);
    payment.setProviderTime(System.currentTimeMillis());
    paymentService.save(payment);
    return transaction(body, payment);
  }

  private ResponseEntity<?> confirm(JsonNode body) {
    Optional<Payment> found = paymentService.lockByTransaction(CODE, body.path("transId").asText());
    if (found.isEmpty()) {
      return error(body, UzumError.TRANSACTION_NOT_FOUND);
    }
    Payment payment = found.get();
    if (payment.getStatus() == PaymentStatus.CANCELLED) {
      return error(body, UzumError.CANCELLED);
    }
    paymentService.markPaid(payment);
    return transaction(body, payment);
  }

  private ResponseEntity<?> reverse(JsonNode body) {
    Optional<Payment> found = paymentService.lockByTransaction(CODE, body.path("transId").asText());
    if (found.isEmpty()) {
      return error(body, UzumError.TRANSACTION_NOT_FOUND);
    }
    Payment payment = found.get();
    if (payment.getStatus() == PaymentStatus.PAID) {
      // The plan is already switched on; refunds are handled by hand.
      return error(body, UzumError.CANNOT_REVERSE);
    }
    paymentService.cancel(payment, null);
    return transaction(body, payment);
  }

  private ResponseEntity<?> status(JsonNode body) {
    return paymentService.lockByTransaction(CODE, body.path("transId").asText())
        .<ResponseEntity<?>>map(payment -> transaction(body, payment))
        .orElseGet(() -> error(body, UzumError.TRANSACTION_NOT_FOUND));
  }

  /** The transaction answer shared by create, confirm, reverse and status. */
  private ResponseEntity<?> transaction(JsonNode body, Payment payment) {
    Map<String, Object> response = base(body);
    response.put("transId", payment.getProviderTransactionId());
    response.put("status", switch (payment.getStatus()) {
      case PENDING -> "CREATED";
      case PAID -> "CONFIRMED";
      case CANCELLED -> "REVERSED";
    });
    response.put("transTime", payment.getProviderTime());
    response.put("confirmTime", millis(payment.getPaidAt()));
    response.put("reverseTime", millis(payment.getCancelledAt()));
    response.put("data", data(payment));
    response.put("amount", tiyin(payment));
    return ResponseEntity.ok(response);
  }

  private Optional<Payment> order(JsonNode body) {
    return paymentService.lockByOrder(body.path("params").path(accountField()).asText(null))
        .filter(payment -> CODE.equals(payment.getProvider()));
  }

  private Map<String, Object> data(Payment payment) {
    return Map.of(accountField(), Map.of("value", String.valueOf(payment.getOrderNumber())));
  }

  private Map<String, Object> base(JsonNode body) {
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("serviceId", body == null ? null : body.path("serviceId").asLong());
    response.put("timestamp", System.currentTimeMillis());
    return response;
  }

  private ResponseEntity<?> error(JsonNode body, UzumError error) {
    Map<String, Object> response = base(body);
    response.put("status", "FAILED");
    response.put("errorCode", error.code());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  private static Long millis(Instant instant) {
    return instant == null ? null : instant.toEpochMilli();
  }

  private static long tiyin(Payment payment) {
    return payment.getAmount() * 100;
  }

  private String accountField() {
    return properties.value(CODE, "account-field", "order_id");
  }

  private String serviceId() {
    return properties.value(CODE, "service-id");
  }
}
