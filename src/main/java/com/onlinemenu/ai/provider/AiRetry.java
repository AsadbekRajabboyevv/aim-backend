package com.onlinemenu.ai.provider;

import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

@Slf4j
public final class AiRetry {

  private static final long[] DELAYS_MS = {1_000, 3_000};

  private AiRetry() {
  }

  public static <T> T onUnavailable(String vendor, Supplier<T> call) {
    for (int attempt = 0; ; attempt++) {
      try {
        return call.get();
      } catch (AiProviderException ex) {
        if (ex.getKind() != AiProviderException.Kind.UNAVAILABLE || attempt >= DELAYS_MS.length) {
          throw ex;
        }
        log.info("{} is unavailable, retrying in {} ms: {}", vendor, DELAYS_MS[attempt],
            ex.getMessage());
        try {
          Thread.sleep(DELAYS_MS[attempt]);
        } catch (InterruptedException interrupted) {
          Thread.currentThread().interrupt();
          throw ex;
        }
      }
    }
  }
}
