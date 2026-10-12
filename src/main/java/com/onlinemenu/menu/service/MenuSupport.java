package com.onlinemenu.menu.service;

import com.onlinemenu.cafe.domain.Cafe;
import com.onlinemenu.cafe.repository.CafeRepository;
import com.onlinemenu.common.constant.ExceptionMessageCodes;
import com.onlinemenu.common.exception.ResourceNotFoundException;
import com.onlinemenu.common.util.ActiveLanguageProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
class MenuSupport {

  private final CafeRepository cafeRepository;
  private final ActiveLanguageProvider activeLanguageProvider;

  Cafe findCafeOrThrow(String cafeSlug) {
    return cafeRepository.findBySlug(cafeSlug)
        .orElseThrow(
            () -> new ResourceNotFoundException(ExceptionMessageCodes.CAFE_NOT_FOUND, cafeSlug));
  }

  String primaryName(Map<String, String> translations, String fallback) {
    String name = translations.get(activeLanguageProvider.getDefaultLanguageCode());
    if (name != null && !name.isBlank()) {
      return name;
    }
    return translations.values().stream().filter(v -> v != null && !v.isBlank()).findFirst()
        .orElse(fallback);
  }

  String defaultLanguageCode() {
    return activeLanguageProvider.getDefaultLanguageCode();
  }
}
