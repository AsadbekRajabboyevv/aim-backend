package com.onlinemenu.billing.domain;

import java.util.Locale;
import java.util.Set;

/**
 * Billing currency, picked from the user's country: Uzbekistan pays in so'm, the euro area in
 * euros, everyone else in US dollars.
 *
 * <p>Amounts are stored in the smallest unit people pay in: whole so'm for UZS, cents for USD and
 * EUR.
 */
public enum Currency {
  UZS,
  USD,
  EUR;

  public static final String UZBEKISTAN = "UZ";

  /** Countries whose currency is the euro (including microstates and countries using it unilaterally). */
  private static final Set<String> EURO_COUNTRIES = Set.of(
      "AT", "BE", "BG", "HR", "CY", "EE", "FI", "FR", "DE", "GR", "IE", "IT", "LV", "LT", "LU",
      "MT", "NL", "PT", "SK", "SI", "ES", "AD", "MC", "SM", "VA", "ME", "XK");

  /** UZS when the country is unknown: every account created before countries existed is Uzbek. */
  public static Currency forCountry(String country) {
    if (country == null || country.isBlank()) {
      return UZS;
    }
    String code = country.trim().toUpperCase(Locale.ROOT);
    if (UZBEKISTAN.equals(code)) {
      return UZS;
    }
    return EURO_COUNTRIES.contains(code) ? EUR : USD;
  }
}
