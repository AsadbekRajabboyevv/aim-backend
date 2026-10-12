package com.onlinemenu.menu.dto;

import java.util.List;

public record ProductBulkAvailabilityRequestDto(
    boolean available,
    List<String> slugs
) {

}
