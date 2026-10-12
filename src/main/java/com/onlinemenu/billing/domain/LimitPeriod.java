package com.onlinemenu.billing.domain;

import java.time.LocalDate;

public enum LimitPeriod {
  NONE,
  DAY,
  MONTH;

  private static final LocalDate LIFETIME = LocalDate.of(1970, 1, 1);

  public LocalDate start(LocalDate today) {
    return switch (this) {
      case NONE -> LIFETIME;
      case DAY -> today;
      case MONTH -> today.withDayOfMonth(1);
    };
  }
}
