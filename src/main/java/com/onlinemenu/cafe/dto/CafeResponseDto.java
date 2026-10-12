package com.onlinemenu.cafe.dto;

import com.onlinemenu.cafe.domain.MenuTheme;

import java.time.Instant;
import java.util.Map;

public record CafeResponseDto(
    String slug,
    Map<String, String> nameTranslations,
    Map<String, String> descriptionTranslations,
    String logoUrl,
    String coverUrl,
    String phone,
    String address,
    String email,
    String workingHours,
    String wifiName,
    String wifiPassword,
    Map<String, String> socialLinks,
    MenuTheme theme,
    boolean active,
    String ownerSlug,
    String ownerFullName,
    Instant createdAt
) {

}
