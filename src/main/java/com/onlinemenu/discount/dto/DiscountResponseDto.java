package com.onlinemenu.discount.dto;

import com.onlinemenu.discount.domain.DiscountStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record DiscountResponseDto(
    String slug,
    String cafeSlug,
    Map<String, String> cafeNameTranslations,
    String productSlug,
    Map<String, String> productNameTranslations,
    String productImageUrl,
    BigDecimal productPrice,
    BigDecimal discountPrice,
    int percent,
    Instant startsAt,
    Instant endsAt,
    boolean active,
    DiscountStatus status,
    Instant createdAt
) {

}
