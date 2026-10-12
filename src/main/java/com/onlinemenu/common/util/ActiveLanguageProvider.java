package com.onlinemenu.common.util;

public interface ActiveLanguageProvider {

  boolean isActive(String languageCode);

  String getDefaultLanguageCode();
}
