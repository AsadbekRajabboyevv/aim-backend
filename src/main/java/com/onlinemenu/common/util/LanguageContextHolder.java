package com.onlinemenu.common.util;

import com.onlinemenu.common.constant.AppConstants;

public final class LanguageContextHolder {

  private static final ThreadLocal<String> CURRENT_LANGUAGE = new ThreadLocal<>();

  private LanguageContextHolder() {
  }

  public static void set(String languageCode) {
    CURRENT_LANGUAGE.set(languageCode);
  }

  public static String get() {
    String languageCode = CURRENT_LANGUAGE.get();
    return languageCode != null ? languageCode : AppConstants.FALLBACK_LANGUAGE_CODE;
  }

  public static void clear() {
    CURRENT_LANGUAGE.remove();
  }
}
