package com.onlinemenu.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(Map<String, Provider> providers, Integer requestTimeoutSeconds) {

  public AiProperties {
    providers = providers == null ? new HashMap<>() : providers;
    requestTimeoutSeconds = requestTimeoutSeconds == null ? 90 : requestTimeoutSeconds;
  }

  public Provider provider(String id) {
    return providers.getOrDefault(id, new Provider(null, null));
  }

  public record Provider(String apiKey, String baseUrl) {

  }
}
