package com.onlinemenu.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.onlinemenu.common.constant.AppConstants;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

  @Bean
  public CacheManager cacheManager() {
    CaffeineCacheManager cacheManager = new CaffeineCacheManager(
        AppConstants.CACHE_ACTIVE_LANGUAGES,
        AppConstants.CACHE_DEFAULT_LANGUAGE,
        AppConstants.CACHE_SYSTEM_MESSAGES,
        AppConstants.CACHE_SETTINGS
    );
    cacheManager.setCaffeine(Caffeine.newBuilder()
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .maximumSize(500));
    return cacheManager;
  }
}
