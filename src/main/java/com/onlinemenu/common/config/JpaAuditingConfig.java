package com.onlinemenu.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Configuration
public class JpaAuditingConfig {

  private static final String SYSTEM_AUDITOR = "system";

  @Bean
  public AuditorAware<String> auditorAware() {
    return () -> {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication == null || !authentication.isAuthenticated()
          || "anonymousUser".equals(authentication.getPrincipal())) {
        return Optional.of(SYSTEM_AUDITOR);
      }
      return Optional.of(authentication.getName());
    };
  }
}
