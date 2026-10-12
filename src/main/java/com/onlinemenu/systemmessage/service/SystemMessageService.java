package com.onlinemenu.systemmessage.service;

import com.onlinemenu.common.util.MessageResolver;
import com.onlinemenu.systemmessage.dto.SystemMessageFilter;
import com.onlinemenu.systemmessage.dto.SystemMessageRequestDto;
import com.onlinemenu.systemmessage.dto.SystemMessageResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface SystemMessageService extends MessageResolver {

  Page<SystemMessageResponseDto> get(SystemMessageFilter filter, Pageable pageable);

  SystemMessageResponseDto get(String code);

  Map<String, Map<String, String>> getTranslationsByPrefix(String prefix);

  /** Texts of every message under {@code prefix} in one language (default language as fallback), keyed without the prefix. */
  Map<String, String> resolveByPrefix(String prefix, String languageCode);

  SystemMessageResponseDto create(SystemMessageRequestDto dto);

  SystemMessageResponseDto update(String code, SystemMessageRequestDto dto);

  void delete(String code);
}
