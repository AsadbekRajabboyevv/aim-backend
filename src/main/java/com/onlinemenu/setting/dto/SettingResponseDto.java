package com.onlinemenu.setting.dto;

import com.onlinemenu.setting.domain.SettingType;

import java.time.Instant;

public record SettingResponseDto(
    String key,
    String value,
    SettingType type,
    String description,
    boolean protectedKey,
    Instant updatedAt,
    String updatedBy
) {

}
