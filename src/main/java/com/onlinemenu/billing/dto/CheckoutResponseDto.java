package com.onlinemenu.billing.dto;

import com.onlinemenu.billing.domain.Currency;

public record CheckoutResponseDto(
    long orderNumber,
    long amount,
    Currency currency,
    String provider,
    String redirectUrl
) {

}
