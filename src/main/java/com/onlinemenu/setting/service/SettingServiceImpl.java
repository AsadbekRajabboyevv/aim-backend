package com.onlinemenu.setting.service;

import com.onlinemenu.common.constant.AppConstants;
import com.onlinemenu.common.constant.ExceptionMessageCodes;
import com.onlinemenu.common.constant.SettingKeys;
import com.onlinemenu.common.exception.BusinessException;
import com.onlinemenu.common.exception.DuplicateResourceException;
import com.onlinemenu.common.exception.ResourceNotFoundException;
import com.onlinemenu.setting.domain.Setting;
import com.onlinemenu.setting.domain.SettingType;
import com.onlinemenu.setting.dto.SettingCreateRequestDto;
import com.onlinemenu.setting.dto.SettingFilter;
import com.onlinemenu.setting.dto.SettingMapper;
import com.onlinemenu.setting.dto.SettingRequestDto;
import com.onlinemenu.setting.dto.SettingResponseDto;
import com.onlinemenu.setting.repository.SettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettingServiceImpl implements SettingService {

  private final SettingRepository settingRepository;
  private final SettingMapper settingMapper;
  private final CacheManager cacheManager;

  @Override
  public Page<SettingResponseDto> get(SettingFilter filter, Pageable pageable) {
    return settingRepository.search(filter, pageable).map(settingMapper::toResponseDto);
  }

  @Override
  public SettingResponseDto get(String key) {
    return settingMapper.toResponseDto(findByKeyOrThrow(key));
  }

  @Override
  @Transactional
  public SettingResponseDto create(SettingCreateRequestDto dto) {
    String key = dto.key().trim();
    if (settingRepository.existsByKey(key)) {
      throw new DuplicateResourceException(ExceptionMessageCodes.SETTING_KEY_ALREADY_EXISTS, key);
    }
    Setting setting = Setting.builder()
        .key(key)
        .value(validatedValue(dto.type(), dto.value()))
        .type(dto.type())
        .description(blankToNull(dto.description()))
        .build();
    return settingMapper.toResponseDto(settingRepository.save(setting));
  }

  @Override
  @Transactional
  @CacheEvict(value = AppConstants.CACHE_SETTINGS, key = "#key")
  public SettingResponseDto update(String key, SettingRequestDto dto) {
    Setting setting = findByKeyOrThrow(key);
    SettingType type = dto.type() != null ? dto.type() : setting.getType();
    if (type != setting.getType() && SettingKeys.PROTECTED.contains(key)) {
      throw new BusinessException(ExceptionMessageCodes.SETTING_PROTECTED, HttpStatus.CONFLICT,
          key);
    }
    setting.setValue(validatedValue(type, dto.value()));
    setting.setType(type);
    setting.setDescription(blankToNull(dto.description()));
    return settingMapper.toResponseDto(setting);
  }

  @Override
  @Transactional
  @CacheEvict(value = AppConstants.CACHE_SETTINGS, key = "#key")
  public void delete(String key) {
    if (SettingKeys.PROTECTED.contains(key)) {
      throw new BusinessException(ExceptionMessageCodes.SETTING_PROTECTED, HttpStatus.CONFLICT,
          key);
    }
    settingRepository.delete(findByKeyOrThrow(key));
  }

  @Override
  public String getString(String key, String defaultValue) {
    return rawValue(key, SettingType.STRING).orElse(defaultValue);
  }

  @Override
  public int getInt(String key, int defaultValue) {
    return rawValue(key, SettingType.INTEGER).map(Integer::parseInt).orElse(defaultValue);
  }

  @Override
  public boolean getBoolean(String key, boolean defaultValue) {
    return rawValue(key, SettingType.BOOLEAN).map(Boolean::parseBoolean).orElse(defaultValue);
  }

  /**
   * Cached by key through the CacheManager directly rather than {@code @Cacheable}, because the
   * typed getters above are invoked on {@code this} and would bypass the Spring proxy. Missing or
   * mistyped rows are not cached, so fixing the row takes effect immediately.
   */
  private Optional<String> rawValue(String key, SettingType expectedType) {
    var cache = Objects.requireNonNull(cacheManager.getCache(AppConstants.CACHE_SETTINGS));
    String cached = cache.get(key, String.class);
    if (cached != null && expectedType.isValid(cached)) {
      return Optional.of(cached);
    }
    Optional<String> value = settingRepository.findByKey(key)
        .filter(setting -> {
          boolean matches =
              setting.getType() == expectedType && expectedType.isValid(setting.getValue());
          if (!matches) {
            log.warn("Setting '{}' is not a valid {}, falling back to the default", key,
                expectedType);
          }
          return matches;
        })
        .map(Setting::getValue);
    value.ifPresent(v -> cache.put(key, v));
    return value;
  }

  private String validatedValue(SettingType type, String rawValue) {
    String value = rawValue.trim();
    if (!type.isValid(value)) {
      throw new BusinessException(ExceptionMessageCodes.SETTING_INVALID_VALUE,
          HttpStatus.BAD_REQUEST, type.name());
    }
    return value;
  }

  private String blankToNull(String text) {
    return text == null || text.isBlank() ? null : text.trim();
  }

  private Setting findByKeyOrThrow(String key) {
    return settingRepository.findByKey(key)
        .orElseThrow(
            () -> new ResourceNotFoundException(ExceptionMessageCodes.SETTING_NOT_FOUND, key));
  }
}
