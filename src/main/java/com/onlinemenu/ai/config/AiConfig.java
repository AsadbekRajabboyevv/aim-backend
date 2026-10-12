package com.onlinemenu.ai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiConfig {

  @Bean(destroyMethod = "close")
  public ExecutorService aiStreamExecutor() {
    return Executors.newVirtualThreadPerTaskExecutor();
  }
}
