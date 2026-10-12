package com.onlinemenu.billing.domain;

/**
 * How a plan limit is measured.
 */
public enum LimitKind {
  /** Number of things the account has right now (cafes, products); counted by a UsageCounter. */
  COUNT,
  /** Number of uses per period (AI messages per month); counted in billing_usage. */
  METERED,
  /** On (value > 0) or off. */
  FEATURE
}
