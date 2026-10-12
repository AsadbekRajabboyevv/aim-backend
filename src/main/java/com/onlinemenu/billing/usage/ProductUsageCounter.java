package com.onlinemenu.billing.usage;

import com.onlinemenu.billing.service.LimitKeys;
import com.onlinemenu.menu.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
class ProductUsageCounter implements UsageCounter {

  private final ProductRepository productRepository;

  @Override
  public String key() {
    return LimitKeys.PRODUCTS;
  }

  @Override
  public long count(UUID userId) {
    return productRepository.countByCafeOwnerId(userId);
  }
}
