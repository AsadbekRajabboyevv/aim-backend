package com.onlinemenu.systemmessage.service;

import com.onlinemenu.common.constant.AppConstants;
import com.onlinemenu.common.constant.ExceptionMessageCodes;
import com.onlinemenu.common.exception.DuplicateResourceException;
import com.onlinemenu.common.exception.ResourceNotFoundException;
import com.onlinemenu.common.util.ActiveLanguageProvider;
import com.onlinemenu.systemmessage.domain.SystemMessage;
import com.onlinemenu.systemmessage.dto.SystemMessageFilter;
import com.onlinemenu.systemmessage.dto.SystemMessageMapper;
import com.onlinemenu.systemmessage.dto.SystemMessageRequestDto;
import com.onlinemenu.systemmessage.dto.SystemMessageResponseDto;
import com.onlinemenu.systemmessage.repository.SystemMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SystemMessageServiceImpl implements SystemMessageService {

  private final SystemMessageRepository systemMessageRepository;
  private final SystemMessageMapper systemMessageMapper;
  private final ActiveLanguageProvider activeLanguageProvider;

  @Override
  public Page<SystemMessageResponseDto> get(SystemMessageFilter filter, Pageable pageable) {
    return systemMessageRepository.search(blankToNull(filter.search()), blankToNull(filter.prefix()),
            blankToNull(filter.missingLanguage()), pageable)
        .map(systemMessageMapper::toResponseDto);
  }

  @Override
  public SystemMessageResponseDto get(String code) {
    return systemMessageMapper.toResponseDto(findByCodeOrThrow(code));
  }

  @Override
  public Map<String, Map<String, String>> getTranslationsByPrefix(String prefix) {
    return systemMessageRepository.findAllByCodeStartingWith(prefix).stream()
        .collect(Collectors.toMap(
            message -> message.getCode().substring(prefix.length()),
            SystemMessage::getTranslations));
  }

  @Override
  @Cacheable(value = AppConstants.CACHE_SYSTEM_MESSAGES, key = "'prefix::' + #prefix + '::' + #languageCode")
  public Map<String, String> resolveByPrefix(String prefix, String languageCode) {
    Map<String, String> result = new TreeMap<>();
    for (SystemMessage message : systemMessageRepository.findAllByCodeStartingWith(prefix)) {
      String text = textFor(message.getTranslations(), languageCode);
      if (text != null && !text.isBlank()) {
        result.put(message.getCode().substring(prefix.length()), text);
      }
    }
    return result;
  }

  @Override
  @Transactional
  @CacheEvict(value = AppConstants.CACHE_SYSTEM_MESSAGES, allEntries = true)
  public SystemMessageResponseDto create(SystemMessageRequestDto dto) {
    if (systemMessageRepository.existsByCode(dto.code())) {
      throw new DuplicateResourceException(ExceptionMessageCodes.SYSTEM_MESSAGE_CODE_ALREADY_EXISTS,
          dto.code());
    }
    SystemMessage systemMessage = systemMessageMapper.toEntity(dto);
    return systemMessageMapper.toResponseDto(systemMessageRepository.save(systemMessage));
  }

  @Override
  @Transactional
  @CacheEvict(value = AppConstants.CACHE_SYSTEM_MESSAGES, allEntries = true)
  public SystemMessageResponseDto update(String code, SystemMessageRequestDto dto) {
    SystemMessage systemMessage = findByCodeOrThrow(code);
    systemMessageMapper.updateEntityFromDto(dto, systemMessage);
    return systemMessageMapper.toResponseDto(systemMessage);
  }

  @Override
  @Transactional
  @CacheEvict(value = AppConstants.CACHE_SYSTEM_MESSAGES, allEntries = true)
  public void delete(String code) {
    SystemMessage systemMessage = findByCodeOrThrow(code);
    systemMessageRepository.delete(systemMessage);
  }

  @Override
  @Cacheable(value = AppConstants.CACHE_SYSTEM_MESSAGES, key = "#code + '::' + #languageCode + '::' + T(java.util.Arrays).deepToString(new Object[] {#args})")
  public String resolve(String code, String languageCode, Object... args) {
    return systemMessageRepository.findByCode(code)
        .map(SystemMessage::getTranslations)
        .map(translations -> textFor(translations, languageCode))
        .map(text -> format(text, args))
        .orElse(code);
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private String textFor(Map<String, String> translations, String languageCode) {
    String text = translations.get(languageCode);
    if (text != null && !text.isBlank()) {
      return text;
    }
    return translations.get(activeLanguageProvider.getDefaultLanguageCode());
  }

  private String format(String text, Object... args) {
    if (text == null) {
      return null;
    }
    if (args == null || args.length == 0) {
      return text;
    }
    try {
      return String.format(text, args);
    } catch (Exception ex) {
      log.warn("Failed to format system message '{}' with args, returning raw text", text, ex);
      return text;
    }
  }

  private SystemMessage findByCodeOrThrow(String code) {
    return systemMessageRepository.findByCode(code)
        .orElseThrow(
            () -> new ResourceNotFoundException(ExceptionMessageCodes.SYSTEM_MESSAGE_NOT_FOUND,
                code));
  }
}
