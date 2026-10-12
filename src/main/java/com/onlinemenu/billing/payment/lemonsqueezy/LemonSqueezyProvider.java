package com.onlinemenu.billing.payment.lemonsqueezy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinemenu.billing.domain.Currency;
import com.onlinemenu.billing.domain.Payment;
import com.onlinemenu.billing.domain.PaymentStatus;
import com.onlinemenu.billing.payment.CallbackAuth;
import com.onlinemenu.billing.payment.PaymentProperties;
import com.onlinemenu.billing.payment.PaymentProvider;
import com.onlinemenu.billing.service.PaymentService;
import com.onlinemenu.common.constant.ExceptionMessageCodes;
import com.onlinemenu.common.exception.BusinessException;
import com.onlinemenu.user.domain.User;
import com.onlinemenu.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Lemon Squeezy (card payments outside Uzbekistan, in USD or EUR). A Lemon Squeezy store charges in
 * one currency, so each currency has its own store and product variant; the order is created through
 * the API with the plan price as {@code custom_price} and our order number in {@code custom_data}.
 * Lemon Squeezy then calls the webhook ({@code <callback-url>}, event {@code order_created}) signed
 * with the signing secret in {@code X-Signature}. Docs: https://docs.lemonsqueezy.com/api
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LemonSqueezyProvider implements PaymentProvider {

  static final String CODE = "lemonsqueezy";
  private static final String JSON_API = "application/vnd.api+json";

  private final PaymentProperties properties;
  private final PaymentService paymentService;
  private final UserRepository userRepository;
  private final ObjectMapper objectMapper;

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public String displayName() {
    return "Lemon Squeezy (Visa / Mastercard)";
  }

  @Override
  public boolean isConfigured() {
    return properties.value(CODE, "api-key") != null && properties.value(CODE, "signing-secret") != null
        && (supports(Currency.USD) || supports(Currency.EUR));
  }

  @Override
  public boolean supports(Currency currency) {
    return currency != Currency.UZS && storeId(currency) != null && variantId(currency) != null;
  }

  @Override
  public List<String> envVariables() {
    return List.of("LEMONSQUEEZY_API_KEY", "LEMONSQUEEZY_SIGNING_SECRET",
        "LEMONSQUEEZY_STORE_ID_USD", "LEMONSQUEEZY_VARIANT_ID_USD",
        "LEMONSQUEEZY_STORE_ID_EUR", "LEMONSQUEEZY_VARIANT_ID_EUR");
  }

  @Override
  public String checkoutUrl(Payment payment, String returnUrl) {
    Currency currency = payment.getCurrency();
    String email = userRepository.findById(payment.getUserId()).map(User::getEmail).orElse(null);

    Map<String, Object> checkoutData = new LinkedHashMap<>();
    if (email != null) {
      checkoutData.put("email", email);
    }
    checkoutData.put("custom", Map.of("order_number", String.valueOf(payment.getOrderNumber())));

    Map<String, Object> body = Map.of("data", Map.of(
        "type", "checkouts",
        "attributes", Map.of(
            "custom_price", payment.getAmount(),
            "product_options", Map.of("redirect_url", returnUrl),
            "checkout_data", checkoutData),
        "relationships", Map.of(
            "store", Map.of("data", Map.of("type", "stores", "id", storeId(currency))),
            "variant", Map.of("data", Map.of("type", "variants", "id", variantId(currency))))));

    try {
      JsonNode response = RestClient.create()
          .post()
          .uri(properties.value(CODE, "api-url", "https://api.lemonsqueezy.com/v1") + "/checkouts")
          .header("Authorization", "Bearer " + properties.value(CODE, "api-key"))
          .header("Accept", JSON_API)
          .header("Content-Type", JSON_API)
          .body(objectMapper.writeValueAsString(body))
          .retrieve()
          .body(JsonNode.class);
      String url = response == null ? null : response.path("data").path("attributes").path("url").asText(null);
      if (url == null) {
        throw new IllegalStateException("no checkout url in the response");
      }
      return url;
    } catch (RestClientException | IOException | IllegalStateException ex) {
      log.error("Lemon Squeezy checkout for order {} failed", payment.getOrderNumber(), ex);
      throw new BusinessException(ExceptionMessageCodes.BILLING_PROVIDER_UNAVAILABLE, HttpStatus.BAD_GATEWAY);
    }
  }

  @Override
  @Transactional
  public ResponseEntity<?> handleCallback(String path, HttpServletRequest request) throws IOException {
    if (!isConfigured() || !(path.isEmpty() || "/webhook".equals(path))) {
      return ResponseEntity.notFound().build();
    }
    byte[] raw = request.getInputStream().readAllBytes();
    if (!signatureMatches(raw, request.getHeader("X-Signature"))) {
      log.warn("Lemon Squeezy webhook with a bad signature");
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    JsonNode event = objectMapper.readTree(raw);
    String name = event.path("meta").path("event_name").asText("");
    if (!"order_created".equals(name)) {
      // Refunds and other events are handled in the Lemon Squeezy dashboard; acknowledge them.
      log.info("Lemon Squeezy event {} ignored", name);
      return ResponseEntity.ok().build();
    }

    String orderNumber = event.path("meta").path("custom_data").path("order_number").asText(null);
    JsonNode order = event.path("data");
    JsonNode attributes = order.path("attributes");
    var payment = paymentService.lockByOrder(orderNumber).filter(p -> CODE.equals(p.getProvider()));
    if (payment.isEmpty()) {
      log.warn("Lemon Squeezy order {} references unknown order number {}", order.path("id").asText(), orderNumber);
      return ResponseEntity.ok().build();
    }
    Payment target = payment.get();
    String currency = attributes.path("currency").asText("").toUpperCase(Locale.ROOT);
    long total = attributes.path("total").asLong(-1);
    if (!"paid".equals(attributes.path("status").asText()) || total != target.getAmount()
        || !target.getCurrency().name().equals(currency)) {
      log.warn("Lemon Squeezy order {} for order {} does not match: status={} total={} {}",
          order.path("id").asText(), orderNumber, attributes.path("status").asText(), total, currency);
      return ResponseEntity.ok().build();
    }
    if (target.getStatus() == PaymentStatus.PENDING) {
      target.setProviderTransactionId(order.path("id").asText(null));
      paymentService.markPaid(target);
    }
    return ResponseEntity.ok().build();
  }

  private boolean signatureMatches(byte[] body, String signature) {
    String secret = properties.value(CODE, "signing-secret");
    if (secret == null || signature == null) {
      return false;
    }
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      String expected = HexFormat.of().formatHex(mac.doFinal(body));
      return CallbackAuth.constantTimeEquals(expected, signature.trim().toLowerCase(Locale.ROOT));
    } catch (GeneralSecurityException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private String storeId(Currency currency) {
    return properties.value(CODE, "store-id-" + currency.name().toLowerCase(Locale.ROOT));
  }

  private String variantId(Currency currency) {
    return properties.value(CODE, "variant-id-" + currency.name().toLowerCase(Locale.ROOT));
  }
}
