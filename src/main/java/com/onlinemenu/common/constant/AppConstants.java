package com.onlinemenu.common.constant;


public final class AppConstants {

  private AppConstants() {
  }

  public static final int DEFAULT_PAGE_SIZE = 20;
  public static final String DEFAULT_SORT_FIELD = "createdAt";

  public static final String SLUG_SEPARATOR = "-";
  public static final int SLUG_MAX_COLLISION_ATTEMPTS = 1000;

  public static final String LANGUAGE_HEADER = "X-Lang";
  public static final String FALLBACK_LANGUAGE_CODE = "en";

  public static final String CACHE_ACTIVE_LANGUAGES = "activeLanguages";
  public static final String CACHE_DEFAULT_LANGUAGE = "defaultLanguage";
  public static final String CACHE_SYSTEM_MESSAGES = "systemMessages";
  public static final String CACHE_SETTINGS = "settings";

  public static final String MENU_UI_MESSAGE_PREFIX = "menu.ui.";
  public static final String UI_MESSAGE_PREFIX = "ui.";

  public static final String MENU_TABLE_QUERY_PARAM = "table";
}
