package com.onlinemenu.systemmessage.dto;

import com.onlinemenu.language.dto.LanguageResponseDto;

import java.util.List;
import java.util.Map;

/**
 * Everything the admin UI needs to render in one language.
 *
 * @param language        the language the texts are in (the default one if the requested language is not active)
 * @param defaultLanguage the platform default language
 * @param languages       active languages the user can switch to
 * @param messages        UI texts keyed without the {@code ui.} prefix
 */
public record UiTranslationsDto(
    String language,
    String defaultLanguage,
    List<LanguageResponseDto> languages,
    Map<String, String> messages
) {

}
