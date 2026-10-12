package com.onlinemenu.billing.service;

import com.onlinemenu.billing.domain.Currency;
import com.onlinemenu.billing.domain.Payment;
import com.onlinemenu.billing.domain.PaymentStatus;
import com.onlinemenu.billing.domain.Plan;
import com.onlinemenu.billing.dto.CheckoutRequestDto;
import com.onlinemenu.billing.dto.CheckoutResponseDto;
import com.onlinemenu.billing.dto.PaymentProviderDto;
import com.onlinemenu.billing.payment.PaymentProvider;
import com.onlinemenu.billing.payment.PaymentProviderRegistry;
import com.onlinemenu.billing.repository.PaymentRepository;
import com.onlinemenu.common.constant.ExceptionMessageCodes;
import com.onlinemenu.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Starts a payment: creates the order and asks the chosen provider for its checkout link.
 */
@Service
@RequiredArgsConstructor
public class CheckoutService {

  private final PaymentRepository paymentRepository;
  private final PlanService planService;
  private final PaymentProviderRegistry providerRegistry;
  private final UserCurrency userCurrency;

  public List<PaymentProviderDto> providers(Currency currency) {
    return providerRegistry.available().stream()
        .filter(provider -> provider.supports(currency))
        .map(provider -> new PaymentProviderDto(provider.code(), provider.displayName()))
        .toList();
  }

  @Transactional
  public CheckoutResponseDto checkout(UUID userId, CheckoutRequestDto dto) {
    String country = userCurrency.country(userId);
    if (country == null) {
      // A Google sign-up has not picked a country yet; charging so'm by default could bill a foreigner wrongly.
      throw new BusinessException(ExceptionMessageCodes.BILLING_COUNTRY_REQUIRED, HttpStatus.BAD_REQUEST);
    }
    Currency currency = Currency.forCountry(country);
    Plan plan = planService.findOrThrow(dto.planSlug());
    boolean sold = Arrays.stream(Currency.values()).anyMatch(c -> plan.price(dto.period(), c) > 0);
    if (!plan.isActive() || !sold) {
      throw new BusinessException(ExceptionMessageCodes.BILLING_PLAN_NOT_AVAILABLE,
          HttpStatus.BAD_REQUEST);
    }
    long amount = plan.price(dto.period(), currency);
    if (amount <= 0) {
      throw new BusinessException(ExceptionMessageCodes.BILLING_CURRENCY_UNAVAILABLE,
          HttpStatus.BAD_REQUEST, currency.name());
    }
    PaymentProvider provider = providerRegistry.availableOrThrow(dto.provider());
    if (!provider.supports(currency)) {
      throw new BusinessException(ExceptionMessageCodes.BILLING_PROVIDER_UNAVAILABLE, HttpStatus.BAD_REQUEST);
    }
    Payment payment = paymentRepository.saveAndFlush(Payment.builder()
        .userId(userId)
        .plan(plan)
        .period(dto.period())
        .amount(amount)
        .currency(currency)
        .provider(provider.code())
        .status(PaymentStatus.PENDING)
        .build());
    return new CheckoutResponseDto(payment.getOrderNumber(), amount, currency, provider.code(),
        provider.checkoutUrl(payment, providerRegistry.returnUrl(payment)));
  }
}
