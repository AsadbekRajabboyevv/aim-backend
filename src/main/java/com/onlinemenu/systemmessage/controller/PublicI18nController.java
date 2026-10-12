package com.onlinemenu.systemmessage.controller;

import com.onlinemenu.common.constant.AppConstants;
import com.onlinemenu.common.dto.BaseResponse;
import com.onlinemenu.common.util.ActiveLanguageProvider;
import com.onlinemenu.common.util.LanguageUtils;
import com.onlinemenu.language.service.LanguageService;
import com.onlinemenu.systemmessage.dto.UiTranslationsDto;
import com.onlinemenu.systemmessage.service.SystemMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * UI texts for the login page and the admin panels. Public, because the login page needs them before sign-in.
 */
@RestController
@RequestMapping("/api/v1/public/i18n")
@RequiredArgsConstructor
public class PublicI18nController {

  private final SystemMessageService systemMessageService;
  private final LanguageService languageService;
  private final ActiveLanguageProvider activeLanguageProvider;

  @GetMapping("/{languageCode}")
  public BaseResponse<UiTranslationsDto> get(@PathVariable String languageCode) {
    String defaultLanguage = activeLanguageProvider.getDefaultLanguageCode();
    String requested = LanguageUtils.normalize(languageCode);
    String language = activeLanguageProvider.isActive(requested) ? requested : defaultLanguage;
    return BaseResponse.success(new UiTranslationsDto(
        language,
        defaultLanguage,
        languageService.getActive(),
        systemMessageService.resolveByPrefix(AppConstants.UI_MESSAGE_PREFIX, language)));
  }
}
