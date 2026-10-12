package com.onlinemenu.ai.settings.dto;

import java.util.List;

public record AiSettingsResponseDto(
    boolean enabled,
    String activeProvider,
    List<AiProviderSettingsDto> providers
) {

}
