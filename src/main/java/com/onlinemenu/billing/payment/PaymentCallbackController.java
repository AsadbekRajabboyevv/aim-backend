package com.onlinemenu.billing.payment;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * The single public entry point for payment systems: {@code /api/v1/public/payments/<code>/**}
 * goes to the provider with that code. Each provider checks its own signature or credentials.
 */
@RestController
@RequiredArgsConstructor
public class PaymentCallbackController {

  private final PaymentProviderRegistry registry;

  @RequestMapping({PaymentProviderRegistry.CALLBACK_PATH + "{code}",
      PaymentProviderRegistry.CALLBACK_PATH + "{code}/**"})
  public ResponseEntity<?> callback(@PathVariable String code, HttpServletRequest request)
      throws IOException {
    PaymentProvider provider = registry.find(code).orElse(null);
    if (provider == null) {
      return ResponseEntity.notFound().build();
    }
    String prefix = request.getContextPath() + PaymentProviderRegistry.CALLBACK_PATH + code;
    String uri = request.getRequestURI();
    String path = uri.length() > prefix.length() ? uri.substring(prefix.length()) : "";
    return provider.handleCallback(path, request);
  }
}
