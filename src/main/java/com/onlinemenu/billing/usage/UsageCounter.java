package com.onlinemenu.billing.usage;

import java.util.UUID;

public interface UsageCounter {

  String key();

  long count(UUID userId);
}
