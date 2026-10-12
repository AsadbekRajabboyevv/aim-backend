package com.onlinemenu.common.util;

public interface SettingProvider {

  String getString(String key, String defaultValue);

  int getInt(String key, int defaultValue);

  boolean getBoolean(String key, boolean defaultValue);
}
