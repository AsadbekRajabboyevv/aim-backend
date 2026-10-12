package com.onlinemenu.discount.service;

import com.onlinemenu.cafe.domain.Cafe;
import com.onlinemenu.discount.domain.Discount;
import com.onlinemenu.discount.repository.DiscountRepository;
import com.onlinemenu.menu.domain.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DiscountPricing {

  private final DiscountRepository discountRepository;

  public Map<UUID, BigDecimal> runningPrices(Cafe cafe) {
    Map<UUID, BigDecimal> prices = new HashMap<>();
    for (Discount discount : discountRepository.findRunningForCafe(cafe.getId(), Instant.now())) {
      prices.merge(discount.getProduct().getId(), discount.getDiscountPrice(), BigDecimal::min);
    }
    return prices;
  }

  public BigDecimal priceFor(Product product, Map<UUID, BigDecimal> runningPrices) {
    BigDecimal discountPrice = runningPrices.get(product.getId());
    return discountPrice != null && discountPrice.compareTo(product.getPrice()) < 0 ? discountPrice
        : null;
  }
}
