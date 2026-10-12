package com.onlinemenu.billing.domain;

public enum BillingPeriod {
  MONTHLY(1),
  YEARLY(12);

  private final int months;

  BillingPeriod(int months) {
    this.months = months;
  }

  public int months() {
    return months;
  }
}
