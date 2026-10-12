package com.onlinemenu.systemmessage.dto;

import java.util.Map;

public record SystemMessageResponseDto(
    String code,
    Map<String, String> translations
) {

}
