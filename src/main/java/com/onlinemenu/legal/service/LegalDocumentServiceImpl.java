package com.onlinemenu.legal.service;

import com.onlinemenu.common.constant.ExceptionMessageCodes;
import com.onlinemenu.common.exception.ResourceNotFoundException;
import com.onlinemenu.common.util.ActiveLanguageProvider;
import com.onlinemenu.common.util.LanguageContextHolder;
import com.onlinemenu.legal.domain.LegalDocument;
import com.onlinemenu.legal.dto.LegalDocumentAdminResponseDto;
import com.onlinemenu.legal.dto.LegalDocumentMapper;
import com.onlinemenu.legal.dto.LegalDocumentRequestDto;
import com.onlinemenu.legal.dto.LegalDocumentResponseDto;
import com.onlinemenu.legal.repository.LegalDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LegalDocumentServiceImpl implements LegalDocumentService {

  private final LegalDocumentRepository legalDocumentRepository;
  private final LegalDocumentMapper legalDocumentMapper;
  private final ActiveLanguageProvider activeLanguageProvider;

  @Override
  public LegalDocumentResponseDto getPublic(String slug) {
    LegalDocument document = findBySlugOrThrow(slug);
    String language = pickLanguage(document.getContentTranslations());
    return new LegalDocumentResponseDto(
        document.getSlug(),
        textFor(document.getTitleTranslations(), language),
        textFor(document.getContentTranslations(), language),
        language,
        document.getUpdatedAt());
  }

  @Override
  public List<LegalDocumentAdminResponseDto> getAll() {
    return legalDocumentRepository.findAllByOrderBySlugDesc().stream()
        .map(legalDocumentMapper::toAdminResponseDto)
        .toList();
  }

  @Override
  @Transactional
  public LegalDocumentAdminResponseDto update(String slug, LegalDocumentRequestDto dto) {
    LegalDocument document = findBySlugOrThrow(slug);
    document.setTitleTranslations(clean(dto.titleTranslations()));
    document.setContentTranslations(clean(dto.contentTranslations()));
    return legalDocumentMapper.toAdminResponseDto(legalDocumentRepository.saveAndFlush(document));
  }

  /** The request language if the document has text in it, else the default language, else any filled one. */
  private String pickLanguage(Map<String, String> translations) {
    String requested = LanguageContextHolder.get();
    if (hasText(translations.get(requested))) {
      return requested;
    }
    String defaultLanguage = activeLanguageProvider.getDefaultLanguageCode();
    if (hasText(translations.get(defaultLanguage))) {
      return defaultLanguage;
    }
    return translations.entrySet().stream()
        .filter(entry -> hasText(entry.getValue()))
        .map(Map.Entry::getKey)
        .findFirst()
        .orElse(requested);
  }

  private String textFor(Map<String, String> translations, String language) {
    String text = translations.get(language);
    if (hasText(text)) {
      return text;
    }
    return translations.values().stream().filter(LegalDocumentServiceImpl::hasText).findFirst().orElse("");
  }

  private static Map<String, String> clean(Map<String, String> translations) {
    Map<String, String> result = new HashMap<>();
    translations.forEach((language, text) -> {
      if (language != null && hasText(text)) {
        result.put(language.trim().toLowerCase(), text.strip());
      }
    });
    return result;
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private LegalDocument findBySlugOrThrow(String slug) {
    return legalDocumentRepository.findBySlug(slug)
        .orElseThrow(() -> new ResourceNotFoundException(ExceptionMessageCodes.LEGAL_DOCUMENT_NOT_FOUND, slug));
  }
}
