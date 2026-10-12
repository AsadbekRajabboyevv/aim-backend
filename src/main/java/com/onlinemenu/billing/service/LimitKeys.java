package com.onlinemenu.billing.service;

import java.util.Set;

/**
 * Keys of the limits the code enforces itself. Limits added from the admin panel are enforced by
 * their path pattern instead.
 */
public final class LimitKeys {

  private LimitKeys() {
  }

  public static final String CAFES = "cafes";
  public static final String PRODUCTS = "products";
  public static final String AI_MESSAGES = "ai.messages";
  public static final String AI_ACTIONS = "ai.actions";

  /** METERED and FEATURE limits that code consumes or checks (COUNT ones have UsageCounters). */
  public static final Set<String> CODE_ENFORCED = Set.of(AI_MESSAGES, AI_ACTIONS);
}
