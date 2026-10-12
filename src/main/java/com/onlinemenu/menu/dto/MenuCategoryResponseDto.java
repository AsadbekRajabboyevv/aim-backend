package com.onlinemenu.menu.dto;

import java.util.Map;

public record MenuCategoryResponseDto(
    String slug,
    Map<String, String> nameTranslations,
    int sortOrder,
    boolean active,
    long productCount
) {

}
