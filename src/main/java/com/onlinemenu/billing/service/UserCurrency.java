package com.onlinemenu.billing.service;

import com.onlinemenu.billing.domain.Currency;
import com.onlinemenu.user.domain.User;
import com.onlinemenu.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * The currency a user is billed in, from the country on their account.
 */
@Component
@RequiredArgsConstructor
public class UserCurrency {

  private final UserRepository userRepository;

  public String country(UUID userId) {
    return userRepository.findById(userId).map(User::getCountry).orElse(null);
  }

  public Currency of(UUID userId) {
    return Currency.forCountry(country(userId));
  }
}
