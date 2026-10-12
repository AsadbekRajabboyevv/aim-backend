package com.onlinemenu.common.config;

import com.onlinemenu.common.constant.SecurityConstants;
import com.onlinemenu.security.RestAuthenticationEntryPoint;
import com.onlinemenu.security.jwt.JwtAuthFilter;
import com.onlinemenu.security.oauth2.CustomOAuth2UserService;
import com.onlinemenu.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.onlinemenu.security.oauth2.OAuth2AuthenticationSuccessHandler;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http,
      JwtAuthFilter jwtAuthFilter,
      RestAuthenticationEntryPoint restAuthenticationEntryPoint,
      CustomOAuth2UserService customOAuth2UserService,
      OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler,
      OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler,
      ObjectProvider<ClientRegistrationRepository> clientRegistrations)
      throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .cors(cors -> {
        })
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(
            handler -> handler.authenticationEntryPoint(restAuthenticationEntryPoint))
        .authorizeHttpRequests(auth -> auth
            .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
            .requestMatchers(SecurityConstants.PUBLIC_ENDPOINTS).permitAll()
            .anyRequest().authenticated())
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

    if (clientRegistrations.getIfAvailable() != null) {
      http.oauth2Login(oauth2 -> oauth2
          .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
          .successHandler(oAuth2AuthenticationSuccessHandler)
          .failureHandler(oAuth2AuthenticationFailureHandler));
    }

    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
      throws Exception {
    return configuration.getAuthenticationManager();
  }
}
