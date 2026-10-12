package com.onlinemenu.billing.payment.payme;

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
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Payme (Paycom) Merchant API: JSON-RPC calls from Payme to one callback URL, HTTP Basic auth with
 * the cashbox key. Amounts are in tiyin. Docs: https://developer.help.paycom.uz
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymeProvider implements PaymentProvider {

  static final String CODE = "payme";

  private static final long TIMEOUT_MS = 12 * 60 * 60 * 1000L;
  private static final int REASON_TIMEOUT = 4;

  private static final int STATE_CREATED = 1;
  private static final int STATE_PERFORMED = 2;
  private static final int STATE_CANCELLED = -1;
  private static final int STATE_CANCELLED_AFTER_PERFORM = -2;

  private final PaymentProperties properties;
  private final PaymentService paymentService;
  private final ObjectMapper objectMapper;

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public String displayName() {
    return "Payme";
  }

  @Override
  public boolean isConfigured() {
    return merchantId() != null && secretKey() != null;
  }

  @Override
  public List<String> envVariables() {
    return List.of("PAYME_MERCHANT_ID", "PAYME_SECRET_KEY", "PAYME_CHECKOUT_URL");
  }

  @Override
  public String checkoutUrl(Payment payment, String returnUrl) {
    String params = "m=" + merchantId()
        + ";ac." + accountField() + "=" + payment.getOrderNumber()
        + ";a=" + tiyin(payment)
        + ";c=" + returnUrl;
    return properties.value(CODE, "checkout-url", "https://checkout.paycom.uz") + "/"
        + Base64.getEncoder().encodeToString(params.getBytes(StandardCharsets.UTF_8));
  }

  @Override
  @Transactional
  public ResponseEntity<?> handleCallback(String path, HttpServletRequest request)
      throws IOException {
    JsonNode body;
    try {
      body = objectMapper.readTree(request.getInputStream());
    } catch (IOException ex) {
      return reply(null, error(PaymeError.PARSE_ERROR, null));
    }
    if (body == null || !body.isObject()) {
      return reply(null, error(PaymeError.PARSE_ERROR, null));
    }
    JsonNode id = body.get("id");
    if (!CallbackAuth.basicMatches(request, null, secretKey())) {
      return reply(id, error(PaymeError.INSUFFICIENT_PRIVILEGE, null));
    }
    String method = body.path("method").asText("");
    JsonNode params = body.path("params");
    try {
      Map<String, Object> result = switch (method) {
        case "CheckPerformTransaction" -> checkPerform(params);
        case "CreateTransaction" -> create(params);
        case "PerformTransaction" -> perform(params);
        case "CancelTransaction" -> cancel(params);
        case "CheckTransaction" -> check(params);
        case "GetStatement" -> statement(params);
        default -> throw new PaymeException(PaymeError.METHOD_NOT_FOUND, method);
      };
      return reply(id, Map.of("result", result));
    } catch (PaymeException ex) {
      return reply(id, error(ex.error(), ex.data()));
    }
  }

  private Map<String, Object> checkPerform(JsonNode params) {
    Payment payment = orderFor(params);
    validateNew(payment, params);
    return Map.of("allow", true);
  }

  private Map<String, Object> create(JsonNode params) {
    String transactionId = params.path("id").asText();
    var existing = paymentService.lockByTransaction(CODE, transactionId);
    if (existing.isPresent()) {
      Payment payment = existing.get();
      if (payment.getStatus() != PaymentStatus.PENDING) {
        throw new PaymeException(PaymeError.CANNOT_PERFORM, null);
      }
      if (expired(payment)) {
        paymentService.cancel(payment, REASON_TIMEOUT);
        throw new PaymeException(PaymeError.CANNOT_PERFORM, null);
      }
      return Map.of("create_time", payment.getProviderTime(), "transaction",
          transaction(payment), "state", STATE_CREATED);
    }
    Payment payment = orderFor(params);
    validateNew(payment, params);
    payment.setProviderTransactionId(transactionId);
    payment.setProviderTime(params.path("time").asLong(System.currentTimeMillis()));
    paymentService.save(payment);
    return Map.of("create_time", payment.getProviderTime(), "transaction", transaction(payment),
        "state", STATE_CREATED);
  }

  private Map<String, Object> perform(JsonNode params) {
    Payment payment = transactionFor(params);
    if (payment.getStatus() == PaymentStatus.PENDING) {
      if (expired(payment)) {
        paymentService.cancel(payment, REASON_TIMEOUT);
        throw new PaymeException(PaymeError.CANNOT_PERFORM, null);
      }
      paymentService.markPaid(payment);
    }
    if (payment.getStatus() != PaymentStatus.PAID) {
      throw new PaymeException(PaymeError.CANNOT_PERFORM, null);
    }
    return Map.of("transaction", transaction(payment), "perform_time",
        millis(payment.getPaidAt()), "state", STATE_PERFORMED);
  }

  private Map<String, Object> cancel(JsonNode params) {
    Payment payment = transactionFor(params);
    if (payment.getStatus() == PaymentStatus.PAID) {
      // The plan is already switched on; refunds are handled by hand.
      throw new PaymeException(PaymeError.CANNOT_CANCEL, null);
    }
    paymentService.cancel(payment, params.path("reason").isInt() ? params.get("reason").asInt()
        : null);
    return Map.of("transaction", transaction(payment), "cancel_time",
        millis(payment.getCancelledAt()), "state", state(payment));
  }

  private Map<String, Object> check(JsonNode params) {
    Payment payment = transactionFor(params);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("create_time", payment.getProviderTime());
    result.put("perform_time", millis(payment.getPaidAt()));
    result.put("cancel_time", millis(payment.getCancelledAt()));
    result.put("transaction", transaction(payment));
    result.put("state", state(payment));
    result.put("reason", payment.getCancelReason());
    return result;
  }

  private Map<String, Object> statement(JsonNode params) {
    List<Map<String, Object>> transactions = new ArrayList<>();
    for (Payment payment : paymentService.byProviderTime(CODE, params.path("from").asLong(),
        params.path("to").asLong())) {
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("id", payment.getProviderTransactionId());
      item.put("time", payment.getProviderTime());
      item.put("amount", tiyin(payment));
      item.put("account", Map.of(accountField(), String.valueOf(payment.getOrderNumber())));
      item.put("create_time", payment.getProviderTime());
      item.put("perform_time", millis(payment.getPaidAt()));
      item.put("cancel_time", millis(payment.getCancelledAt()));
      item.put("transaction", transaction(payment));
      item.put("state", state(payment));
      item.put("reason", payment.getCancelReason());
      transactions.add(item);
    }
    return Map.of("transactions", transactions);
  }

  private Payment orderFor(JsonNode params) {
    String order = params.path("account").path(accountField()).asText(null);
    return paymentService.lockByOrder(order)
        .filter(payment -> CODE.equals(payment.getProvider()))
        .orElseThrow(() -> new PaymeException(PaymeError.ORDER_NOT_FOUND, accountField()));
  }

  /** Checks an order before a new Payme transaction is attached to it. */
  private void validateNew(Payment payment, JsonNode params) {
    if (payment.getStatus() != PaymentStatus.PENDING) {
      throw new PaymeException(PaymeError.ORDER_NOT_PAYABLE, accountField());
    }
    if (params.path("amount").asLong(-1) != tiyin(payment)) {
      throw new PaymeException(PaymeError.WRONG_AMOUNT, null);
    }
    if (payment.getProviderTransactionId() != null) {
      throw new PaymeException(PaymeError.ORDER_BUSY, accountField());
    }
  }

  private Payment transactionFor(JsonNode params) {
    return paymentService.lockByTransaction(CODE, params.path("id").asText())
        .orElseThrow(() -> new PaymeException(PaymeError.TRANSACTION_NOT_FOUND, null));
  }

  private static boolean expired(Payment payment) {
    return payment.getProviderTime() != null
        && System.currentTimeMillis() - payment.getProviderTime() > TIMEOUT_MS;
  }

  private static int state(Payment payment) {
    return switch (payment.getStatus()) {
      case PENDING -> STATE_CREATED;
      case PAID -> STATE_PERFORMED;
      case CANCELLED -> payment.getPaidAt() != null ? STATE_CANCELLED_AFTER_PERFORM
          : STATE_CANCELLED;
    };
  }

  private static String transaction(Payment payment) {
    return String.valueOf(payment.getOrderNumber());
  }

  private static long millis(Instant instant) {
    return instant == null ? 0 : instant.toEpochMilli();
  }

  private static long tiyin(Payment payment) {
    return payment.getAmount() * 100;
  }

  private String accountField() {
    return properties.value(CODE, "account-field", "order_id");
  }

  private String merchantId() {
    return properties.value(CODE, "merchant-id");
  }

  private String secretKey() {
    return properties.value(CODE, "secret-key");
  }

  private static Map<String, Object> error(PaymeError error, String data) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("code", error.code());
    body.put("message", error.message());
    if (data != null) {
      body.put("data", data);
    }
    return Map.of("error", body);
  }

  /** Payme expects HTTP 200 with a JSON-RPC body, also for errors. */
  private static ResponseEntity<Map<String, Object>> reply(JsonNode id, Map<String, Object> body) {
    Map<String, Object> response = new LinkedHashMap<>(body);
    response.put("id", id == null || id.isNull() ? null : id.isNumber() ? id.asLong() : id.asText());
    return ResponseEntity.ok(response);
  }
}
