package com.onlinemenu.billing.usage;

import com.onlinemenu.billing.service.LimitKeys;
import com.onlinemenu.cafe.repository.CafeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
class CafeUsageCounter implements UsageCounter {

  private final CafeRepository cafeRepository;

  @Override
  public String key() {
    return LimitKeys.CAFES;
  }

  @Override
  public long count(UUID userId) {
    return cafeRepository.countByOwnerId(userId);
  }
}
