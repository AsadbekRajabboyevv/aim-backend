package com.onlinemenu.discount.dto;

import com.onlinemenu.discount.domain.DiscountStatus;

public record DiscountFilter(
    String search,
    String cafeSlug,
    DiscountStatus status
) {

}
