package com.onlinemenu.ai.settings.service;

import com.onlinemenu.ai.provider.AiChatProvider;
import com.onlinemenu.ai.provider.AiProviderException;
import com.onlinemenu.ai.provider.AiProviderRegistry;
import com.onlinemenu.ai.settings.dto.AiProviderSettingsDto;
import com.onlinemenu.ai.settings.dto.AiSettingsRequestDto;
import com.onlinemenu.ai.settings.dto.AiSettingsResponseDto;
import com.onlinemenu.common.constant.ExceptionMessageCodes;
import com.onlinemenu.common.constant.SettingKeys;
import com.onlinemenu.common.exception.BusinessException;
import com.onlinemenu.common.exception.ResourceNotFoundException;
import com.onlinemenu.setting.domain.SettingType;
import com.onlinemenu.setting.dto.SettingCreateRequestDto;
import com.onlinemenu.setting.dto.SettingRequestDto;
import com.onlinemenu.setting.service.SettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiSettingsService {

  private final AiProviderRegistry providerRegistry;
  private final SettingService settingService;

  public AiSettingsResponseDto get() {
    return new AiSettingsResponseDto(
        settingService.getBoolean(SettingKeys.AI_ENABLED, true),
        providerRegistry.activeId(),
        providerRegistry.all().stream()
            .map(provider -> new AiProviderSettingsDto(provider.id(), provider.displayName(),
                provider.isConfigured(),
                providerRegistry.model(provider), providerRegistry.titleModel(provider),
                provider.defaultModel(), provider.defaultTitleModel()))
            .toList());
  }

  public AiSettingsResponseDto update(AiSettingsRequestDto dto) {
    String active = dto.activeProvider().trim().toLowerCase();
    if (providerRegistry.find(active).isEmpty()) {
      throw new BusinessException(ExceptionMessageCodes.AI_PROVIDER_UNKNOWN, HttpStatus.BAD_REQUEST,
          active);
    }
    upsert(SettingKeys.AI_ENABLED, String.valueOf(dto.enabled()), SettingType.BOOLEAN,
        "AI assistant chat on/off for everyone");
    upsert(SettingKeys.AI_PROVIDER, active, SettingType.STRING, "Active AI vendor adapter id");

    Map<String, AiSettingsRequestDto.Models> models =
        dto.models() == null ? Map.of() : dto.models();
    for (AiChatProvider provider : providerRegistry.all()) {
      AiSettingsRequestDto.Models change = models.get(provider.id());
      if (change == null) {
        continue;
      }
      upsert(SettingKeys.aiModel(provider.id()),
          StringUtils.hasText(change.model()) ? change.model().trim() : provider.defaultModel(),
          SettingType.STRING, provider.displayName() + " model used by the assistant chat");
      upsert(SettingKeys.aiTitleModel(provider.id()),
          StringUtils.hasText(change.titleModel()) ? change.titleModel().trim()
              : provider.defaultTitleModel(),
          SettingType.STRING, provider.displayName() + " model used to name chat conversations");
    }
    return get();
  }

  public List<String> models(String providerId) {
    AiChatProvider provider = providerRegistry.find(providerId)
        .orElseThrow(() -> new BusinessException(ExceptionMessageCodes.AI_PROVIDER_UNKNOWN,
            HttpStatus.BAD_REQUEST, providerId));
    if (!provider.isConfigured()) {
      return List.of();
    }
    try {
      return provider.listModels();
    } catch (AiProviderException ex) {
      log.warn("Listing {} models failed: {}", provider.id(), ex.getMessage());
      throw new BusinessException(ExceptionMessageCodes.AI_PROVIDER_FAILED, HttpStatus.BAD_GATEWAY);
    }
  }

  private void upsert(String key, String value, SettingType type, String description) {
    try {
      settingService.get(key);
      settingService.update(key, new SettingRequestDto(value, null, null));
    } catch (ResourceNotFoundException ex) {
      settingService.create(new SettingCreateRequestDto(key, value, type, description));
    }
  }
}
