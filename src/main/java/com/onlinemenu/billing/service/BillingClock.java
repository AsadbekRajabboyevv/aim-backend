package com.onlinemenu.billing.service;

import com.onlinemenu.billing.domain.BillingPeriod;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Calendar maths in the platform's time zone: limit periods start at local midnight and paid
 * periods last calendar months.
 */
public final class BillingClock {

  public static final ZoneId ZONE = ZoneId.of("Asia/Tashkent");

  private BillingClock() {
  }

  public static LocalDate today() {
    return LocalDate.now(ZONE);
  }

  public static Instant plus(Instant from, BillingPeriod period) {
    return from.atZone(ZONE).plusMonths(period.months()).toInstant();
  }

  /** Whole days left, rounding up so "ends in 5 hours" is 1 day. */
  public static int daysLeft(Instant now, Instant end) {
    long seconds = Duration.between(now, end).getSeconds();
    return seconds <= 0 ? 0 : (int) ((seconds + 86_399) / 86_400);
  }
}
