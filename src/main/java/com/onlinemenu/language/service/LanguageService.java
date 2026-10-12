package com.onlinemenu.language.service;

import com.onlinemenu.common.dto.DropdownDto;
import com.onlinemenu.language.dto.LanguageFilter;
import com.onlinemenu.language.dto.LanguageRequestDto;
import com.onlinemenu.language.dto.LanguageResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface LanguageService {

  Page<LanguageResponseDto> get(LanguageFilter filter, Pageable pageable);

  LanguageResponseDto get(String code);

  List<DropdownDto> get();

  List<LanguageResponseDto> getActive();

  LanguageResponseDto create(LanguageRequestDto dto);

  LanguageResponseDto update(String code, LanguageRequestDto dto);

  void delete(String code);
}
