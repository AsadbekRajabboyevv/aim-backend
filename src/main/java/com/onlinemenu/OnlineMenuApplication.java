package com.onlinemenu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableCaching
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@SpringBootApplication
public class OnlineMenuApplication {

  static void main(String[] args) {
    SpringApplication.run(OnlineMenuApplication.class, args);
  }
}
