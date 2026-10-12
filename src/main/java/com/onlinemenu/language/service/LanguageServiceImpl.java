package com.onlinemenu.language.service;

import com.onlinemenu.common.constant.AppConstants;
import com.onlinemenu.common.constant.ExceptionMessageCodes;
import com.onlinemenu.common.dto.DropdownDto;
import com.onlinemenu.common.exception.BusinessException;
import com.onlinemenu.common.exception.DuplicateResourceException;
import com.onlinemenu.common.exception.ResourceNotFoundException;
import com.onlinemenu.common.util.ActiveLanguageProvider;
import com.onlinemenu.common.util.LanguageUtils;
import com.onlinemenu.language.domain.Language;
import com.onlinemenu.language.dto.LanguageFilter;
import com.onlinemenu.language.dto.LanguageMapper;
import com.onlinemenu.language.dto.LanguageRequestDto;
import com.onlinemenu.language.dto.LanguageResponseDto;
import com.onlinemenu.language.repository.LanguageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LanguageServiceImpl implements LanguageService, ActiveLanguageProvider {

  private final LanguageRepository languageRepository;
  private final LanguageMapper languageMapper;

  @Override
  public Page<LanguageResponseDto> get(LanguageFilter filter, Pageable pageable) {
    return languageRepository.search(filter, pageable);
  }

  @Override
  public LanguageResponseDto get(String code) {
    return languageMapper.toResponseDto(findByCodeOrThrow(code));
  }

  @Override
  public List<DropdownDto> get() {
    return languageRepository.findAllByIsActiveTrueOrderBySortOrderAsc().stream()
        .map(language -> new DropdownDto(language.getCode(), language.getNativeName()))
        .toList();
  }

  @Override
  public List<LanguageResponseDto> getActive() {
    return languageRepository.findAllByIsActiveTrueOrderBySortOrderAsc().stream()
        .map(languageMapper::toResponseDto)
        .toList();
  }

  @Override
  @Transactional
  @CacheEvict(value = {AppConstants.CACHE_ACTIVE_LANGUAGES,
      AppConstants.CACHE_DEFAULT_LANGUAGE}, allEntries = true)
  public LanguageResponseDto create(LanguageRequestDto dto) {
    String normalizedCode = LanguageUtils.normalize(dto.code());
    if (languageRepository.existsByCode(normalizedCode)) {
      throw new DuplicateResourceException(ExceptionMessageCodes.LANGUAGE_CODE_ALREADY_EXISTS,
          normalizedCode);
    }
    if (dto.isDefault() && !dto.isActive()) {
      throw new BusinessException(ExceptionMessageCodes.LANGUAGE_DEFAULT_REQUIRED,
          HttpStatus.CONFLICT, normalizedCode);
    }
    Language language = languageMapper.toEntity(dto);
    language.setCode(normalizedCode);
    applyDefaultFlag(language);
    return languageMapper.toResponseDto(languageRepository.save(language));
  }

  @Override
  @Transactional
  @CacheEvict(value = {AppConstants.CACHE_ACTIVE_LANGUAGES,
      AppConstants.CACHE_DEFAULT_LANGUAGE}, allEntries = true)
  public LanguageResponseDto update(String code, LanguageRequestDto dto) {
    Language language = findByCodeOrThrow(code);
    if (language.isDefault() && (!dto.isDefault() || !dto.isActive())) {
      throw new BusinessException(ExceptionMessageCodes.LANGUAGE_DEFAULT_REQUIRED,
          HttpStatus.CONFLICT, code);
    }
    if (dto.isDefault() && !dto.isActive()) {
      throw new BusinessException(ExceptionMessageCodes.LANGUAGE_DEFAULT_REQUIRED,
          HttpStatus.CONFLICT, code);
    }
    languageMapper.updateEntityFromDto(dto, language);
    applyDefaultFlag(language);
    return languageMapper.toResponseDto(language);
  }

  @Override
  @Transactional
  @CacheEvict(value = {AppConstants.CACHE_ACTIVE_LANGUAGES,
      AppConstants.CACHE_DEFAULT_LANGUAGE}, allEntries = true)
  public void delete(String code) {
    Language language = findByCodeOrThrow(code);
    if (language.isDefault()) {
      throw new BusinessException(ExceptionMessageCodes.LANGUAGE_CANNOT_DELETE_DEFAULT,
          HttpStatus.CONFLICT, code);
    }
    languageRepository.delete(language);
  }

  @Override
  @Cacheable(AppConstants.CACHE_ACTIVE_LANGUAGES)
  public boolean isActive(String languageCode) {
    if (languageCode == null) {
      return false;
    }
    return languageRepository.findByCode(languageCode)
        .map(Language::isActive)
        .orElse(false);
  }

  @Override
  @Cacheable(AppConstants.CACHE_DEFAULT_LANGUAGE)
  public String getDefaultLanguageCode() {
    return languageRepository.findByIsDefaultTrue()
        .map(Language::getCode)
        .orElse(AppConstants.FALLBACK_LANGUAGE_CODE);
  }

  private Language findByCodeOrThrow(String code) {
    return languageRepository.findByCode(code)
        .orElseThrow(
            () -> new ResourceNotFoundException(ExceptionMessageCodes.LANGUAGE_NOT_FOUND, code));
  }

  private void applyDefaultFlag(Language language) {
    if (language.isDefault()) {
      languageRepository.findByIsDefaultTrue()
          .filter(existing -> !existing.getId().equals(language.getId()))
          .ifPresent(existing -> {
            existing.setDefault(false);
            languageRepository.save(existing);
          });
    }
  }
}
