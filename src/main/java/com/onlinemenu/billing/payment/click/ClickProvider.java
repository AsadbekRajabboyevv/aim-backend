package com.onlinemenu.billing.payment.click;

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
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Click SHOP API: Prepare (action=0) and Complete (action=1) form posts to the same callback URL,
 * signed with MD5 over the secret key. Amounts are in so'm. Docs: https://docs.click.uz
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClickProvider implements PaymentProvider {

  static final String CODE = "click";

  private static final int OK = 0;
  private static final int SIGN_FAILED = -1;
  private static final int WRONG_AMOUNT = -2;
  private static final int ACTION_NOT_FOUND = -3;
  private static final int ALREADY_PAID = -4;
  private static final int ORDER_NOT_FOUND = -5;
  private static final int TRANSACTION_NOT_FOUND = -6;
  private static final int BAD_REQUEST = -8;
  private static final int CANCELLED = -9;

  private final PaymentProperties properties;
  private final PaymentService paymentService;

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public String displayName() {
    return "Click";
  }

  @Override
  public boolean isConfigured() {
    return serviceId() != null && merchantId() != null && secretKey() != null;
  }

  @Override
  public List<String> envVariables() {
    return List.of("CLICK_SERVICE_ID", "CLICK_MERCHANT_ID", "CLICK_SECRET_KEY",
        "CLICK_MERCHANT_USER_ID", "CLICK_CHECKOUT_URL");
  }

  @Override
  public String checkoutUrl(Payment payment, String returnUrl) {
    var url = UriComponentsBuilder.fromHttpUrl(
            properties.value(CODE, "checkout-url", "https://my.click.uz/services/pay"))
        .queryParam("service_id", serviceId())
        .queryParam("merchant_id", merchantId())
        .queryParam("amount", payment.getAmount())
        .queryParam("transaction_param", payment.getOrderNumber())
        .queryParam("return_url", returnUrl);
    String merchantUserId = properties.value(CODE, "merchant-user-id");
    if (merchantUserId != null) {
      url.queryParam("merchant_user_id", merchantUserId);
    }
    return url.encode().toUriString();
  }

  @Override
  @Transactional
  public ResponseEntity<?> handleCallback(String path, HttpServletRequest request) {
    Map<String, String> params = new LinkedHashMap<>();
    request.getParameterMap().forEach((key, values) -> params.put(key,
        values.length > 0 ? values[0] : null));
    String action = params.getOrDefault("action", "");
    if (!action.equals("0") && !action.equals("1")) {
      return reply(params, ACTION_NOT_FOUND, "Action not found", null);
    }
    boolean complete = action.equals("1");
    String expectedSign = CallbackAuth.md5Hex(params.getOrDefault("click_trans_id", "")
        + params.getOrDefault("service_id", "")
        + secretKey()
        + params.getOrDefault("merchant_trans_id", "")
        + (complete ? params.getOrDefault("merchant_prepare_id", "") : "")
        + params.getOrDefault("amount", "")
        + action
        + params.getOrDefault("sign_time", ""));
    if (!isConfigured()
        || !CallbackAuth.constantTimeEquals(expectedSign, params.get("sign_string"))) {
      return reply(params, SIGN_FAILED, "SIGN CHECK FAILED!", null);
    }

    Optional<Payment> found = paymentService.lockByOrder(params.get("merchant_trans_id"))
        .filter(payment -> CODE.equals(payment.getProvider()));
    if (found.isEmpty()) {
      return reply(params, ORDER_NOT_FOUND, "Order does not exist", null);
    }
    Payment payment = found.get();
    if (payment.getStatus() == PaymentStatus.PAID) {
      return reply(params, ALREADY_PAID, "Already paid", payment);
    }
    if (payment.getStatus() == PaymentStatus.CANCELLED) {
      return reply(params, CANCELLED, "Transaction cancelled", payment);
    }
    if (!amountMatches(params.get("amount"), payment)) {
      return reply(params, WRONG_AMOUNT, "Incorrect parameter amount", payment);
    }
    return complete ? complete(params, payment) : prepare(params, payment);
  }

  private ResponseEntity<?> prepare(Map<String, String> params, Payment payment) {
    payment.setProviderTransactionId(params.get("click_trans_id"));
    payment.setProviderTime(System.currentTimeMillis());
    paymentService.save(payment);
    return reply(params, OK, "Success", payment);
  }

  private ResponseEntity<?> complete(Map<String, String> params, Payment payment) {
    if (!String.valueOf(payment.getOrderNumber()).equals(params.get("merchant_prepare_id"))) {
      return reply(params, TRANSACTION_NOT_FOUND, "Transaction does not exist", payment);
    }
    int clickError;
    try {
      clickError = Integer.parseInt(params.getOrDefault("error", "0"));
    } catch (NumberFormatException ex) {
      return reply(params, BAD_REQUEST, "Error in request from click", payment);
    }
    if (clickError < 0) {
      paymentService.cancel(payment, clickError);
      return reply(params, CANCELLED, "Transaction cancelled", payment);
    }
    paymentService.markPaid(payment);
    return reply(params, OK, "Success", payment);
  }

  private static boolean amountMatches(String amount, Payment payment) {
    try {
      return new BigDecimal(amount).compareTo(BigDecimal.valueOf(payment.getAmount())) == 0;
    } catch (NumberFormatException | NullPointerException ex) {
      return false;
    }
  }

  private static ResponseEntity<Map<String, Object>> reply(Map<String, String> params, int error,
      String note, Payment payment) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("click_trans_id", params.get("click_trans_id"));
    body.put("merchant_trans_id", params.get("merchant_trans_id"));
    String id = payment == null ? null : String.valueOf(payment.getOrderNumber());
    body.put("1".equals(params.get("action")) ? "merchant_confirm_id" : "merchant_prepare_id", id);
    body.put("error", error);
    body.put("error_note", note);
    return ResponseEntity.ok(body);
  }

  private String serviceId() {
    return properties.value(CODE, "service-id");
  }

  private String merchantId() {
    return properties.value(CODE, "merchant-id");
  }

  private String secretKey() {
    return properties.value(CODE, "secret-key");
  }
}
