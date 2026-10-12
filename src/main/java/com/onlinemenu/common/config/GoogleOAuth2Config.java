package com.onlinemenu.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

@Configuration
@ConditionalOnExpression("!'${app.oauth2.google.client-id:}'.isBlank()")
public class GoogleOAuth2Config {

  private static final String REGISTRATION_ID = "google";

  @Bean
  public ClientRegistrationRepository clientRegistrationRepository(
      @Value("${app.oauth2.google.client-id}") String clientId,
      @Value("${app.oauth2.google.client-secret}") String clientSecret) {
    return new InMemoryClientRegistrationRepository(
        CommonOAuth2Provider.GOOGLE.getBuilder(REGISTRATION_ID)
            .clientId(clientId)
            .clientSecret(clientSecret)
            .scope("email", "profile")
            .build());
  }
}
