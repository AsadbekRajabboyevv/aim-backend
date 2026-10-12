package com.onlinemenu.language.dto;

public record LanguageResponseDto(
    String code,
    String name,
    String nativeName,
    boolean isDefault,
    boolean isActive,
    int sortOrder
) {

}
