package com.onlinemenu.ai.provider;

import com.onlinemenu.common.constant.ExceptionMessageCodes;
import com.onlinemenu.common.constant.SettingKeys;
import com.onlinemenu.common.exception.BusinessException;
import com.onlinemenu.common.util.SettingProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AiProviderRegistry {

  public static final String DEFAULT_PROVIDER = "gemini";

  private final List<AiChatProvider> providers;
  private final SettingProvider settingProvider;

  public AiChatProvider active() {
    return find(activeId()).filter(AiChatProvider::isConfigured)
        .orElseThrow(() -> new BusinessException(ExceptionMessageCodes.AI_NOT_CONFIGURED,
            HttpStatus.SERVICE_UNAVAILABLE));
  }

  public boolean isActiveConfigured() {
    return find(activeId()).map(AiChatProvider::isConfigured).orElse(false);
  }

  public String activeId() {
    return settingProvider.getString(SettingKeys.AI_PROVIDER, DEFAULT_PROVIDER).trim()
        .toLowerCase();
  }

  public Optional<AiChatProvider> find(String id) {
    return providers.stream().filter(provider -> provider.id().equals(id)).findFirst();
  }

  public List<AiChatProvider> all() {
    return providers.stream().sorted(Comparator.comparing(AiChatProvider::id)).toList();
  }

  public String model(AiChatProvider provider) {
    return settingProvider.getString(SettingKeys.aiModel(provider.id()), provider.defaultModel());
  }

  public String titleModel(AiChatProvider provider) {
    return settingProvider.getString(SettingKeys.aiTitleModel(provider.id()),
        provider.defaultTitleModel());
  }
}
