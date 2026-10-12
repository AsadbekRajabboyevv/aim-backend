package com.onlinemenu.common.util;

public interface MessageResolver {

  String resolve(String code, String languageCode, Object... args);
}
