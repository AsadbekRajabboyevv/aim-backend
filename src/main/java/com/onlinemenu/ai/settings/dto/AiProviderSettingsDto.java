package com.onlinemenu.ai.settings.dto;

public record AiProviderSettingsDto(
    String id,
    String displayName,
    boolean configured,
    String model,
    String titleModel,
    String defaultModel,
    String defaultTitleModel
) {

}
