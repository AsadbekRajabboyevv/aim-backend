package com.onlinemenu.common.util;

import com.onlinemenu.common.constant.AppConstants;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

import java.util.Locale;

public final class LanguageUtils {

  private LanguageUtils() {
  }

  public static String extractRequestedLanguage(HttpServletRequest request) {
    String header = request.getHeader(AppConstants.LANGUAGE_HEADER);
    if (StringUtils.hasText(header)) {
      return normalize(header);
    }

    Locale locale = request.getLocale();
    if (locale != null && StringUtils.hasText(locale.getLanguage())) {
      return normalize(locale.getLanguage());
    }

    return null;
  }

  public static String normalize(String languageCode) {
    return languageCode == null ? null : languageCode.trim().toLowerCase(Locale.ROOT);
  }
}
