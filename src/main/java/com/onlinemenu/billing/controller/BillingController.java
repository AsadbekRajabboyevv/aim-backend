package com.onlinemenu.billing.controller;

import com.onlinemenu.billing.domain.BillingPeriod;
import com.onlinemenu.billing.dto.BillingSummaryDto;
import com.onlinemenu.billing.dto.CheckoutRequestDto;
import com.onlinemenu.billing.dto.CheckoutResponseDto;
import com.onlinemenu.billing.dto.PaymentProviderDto;
import com.onlinemenu.billing.dto.PaymentResponseDto;
import com.onlinemenu.billing.dto.PlanCatalogDto;
import com.onlinemenu.billing.dto.QuoteDto;
import com.onlinemenu.billing.service.CheckoutService;
import com.onlinemenu.billing.service.EntitlementService;
import com.onlinemenu.billing.service.PaymentService;
import com.onlinemenu.billing.service.PlanService;
import com.onlinemenu.billing.service.SubscriptionService;
import com.onlinemenu.billing.service.UserCurrency;
import com.onlinemenu.common.dto.BaseResponse;
import com.onlinemenu.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The signed-in user's plan, usage and payments.
 */
@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
@PreAuthorize("""
    hasRole(T(com.onlinemenu.common.constant.SecurityConstants).ROLE_SUPER_ADMIN)
    or hasRole(T(com.onlinemenu.common.constant.SecurityConstants).ROLE_CAFE_ADMIN)
    """)
public class BillingController {

  private final EntitlementService entitlementService;
  private final PlanService planService;
  private final SubscriptionService subscriptionService;
  private final PaymentService paymentService;
  private final CheckoutService checkoutService;
  private final UserCurrency userCurrency;

  @GetMapping("/me")
  public BaseResponse<BillingSummaryDto> me(@AuthenticationPrincipal UserPrincipal principal) {
    return BaseResponse.success(entitlementService.summary(principal.getId()));
  }

  @GetMapping("/plans")
  public BaseResponse<PlanCatalogDto> plans(@AuthenticationPrincipal UserPrincipal principal) {
    return BaseResponse.success(planService.catalog(userCurrency.of(principal.getId())));
  }

  /** Payment systems that accept the user's currency. */
  @GetMapping("/providers")
  public BaseResponse<List<PaymentProviderDto>> providers(@AuthenticationPrincipal UserPrincipal principal) {
    return BaseResponse.success(checkoutService.providers(userCurrency.of(principal.getId())));
  }

  @GetMapping("/quote")
  @PreAuthorize("hasRole(T(com.onlinemenu.common.constant.SecurityConstants).ROLE_CAFE_ADMIN)")
  public BaseResponse<QuoteDto> quote(@RequestParam String plan,
      @RequestParam BillingPeriod period, @AuthenticationPrincipal UserPrincipal principal) {
    return BaseResponse.success(
        subscriptionService.quote(principal.getId(), planService.findOrThrow(plan), period));
  }

  @PostMapping("/checkout")
  @PreAuthorize("hasRole(T(com.onlinemenu.common.constant.SecurityConstants).ROLE_CAFE_ADMIN)")
  public BaseResponse<CheckoutResponseDto> checkout(@Valid @RequestBody CheckoutRequestDto dto,
      @AuthenticationPrincipal UserPrincipal principal) {
    return BaseResponse.success(checkoutService.checkout(principal.getId(), dto));
  }

  @GetMapping("/payments")
  public BaseResponse<List<PaymentResponseDto>> payments(
      @AuthenticationPrincipal UserPrincipal principal) {
    return BaseResponse.success(paymentService.get(principal.getId()));
  }

  @GetMapping("/payments/{orderNumber}")
  public BaseResponse<PaymentResponseDto> payment(@PathVariable long orderNumber,
      @AuthenticationPrincipal UserPrincipal principal) {
    return BaseResponse.success(paymentService.get(principal.getId(), orderNumber));
  }
}
