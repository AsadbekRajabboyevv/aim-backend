package com.onlinemenu.discount.domain;

import java.time.Instant;

public enum DiscountStatus {
  ACTIVE,
  SCHEDULED,
  EXPIRED,
  DISABLED;

  public static DiscountStatus of(Discount discount, Instant now) {
    if (!discount.isActive()) {
      return DISABLED;
    }
    if (discount.getStartsAt() != null && discount.getStartsAt().isAfter(now)) {
      return SCHEDULED;
    }
    if (discount.getEndsAt() != null && !discount.getEndsAt().isAfter(now)) {
      return EXPIRED;
    }
    return ACTIVE;
  }
}
