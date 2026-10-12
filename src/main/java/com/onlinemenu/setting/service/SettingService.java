package com.onlinemenu.setting.service;

import com.onlinemenu.common.util.SettingProvider;
import com.onlinemenu.setting.dto.SettingCreateRequestDto;
import com.onlinemenu.setting.dto.SettingFilter;
import com.onlinemenu.setting.dto.SettingRequestDto;
import com.onlinemenu.setting.dto.SettingResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SettingService extends SettingProvider {

  Page<SettingResponseDto> get(SettingFilter filter, Pageable pageable);

  SettingResponseDto get(String key);

  SettingResponseDto create(SettingCreateRequestDto dto);

  SettingResponseDto update(String key, SettingRequestDto dto);

  void delete(String key);
}
