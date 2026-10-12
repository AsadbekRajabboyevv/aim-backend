package com.onlinemenu.cafe.dto;

import java.util.List;

public record MyCafesResponseDto(
    List<CafeResponseDto> cafes,
    /* Null when the plan has no cafe limit. */
    Long maxCafes
) {

}
