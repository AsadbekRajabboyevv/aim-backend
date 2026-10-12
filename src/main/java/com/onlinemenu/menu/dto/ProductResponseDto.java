package com.onlinemenu.menu.dto;

import com.onlinemenu.menu.domain.ProductTag;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ProductResponseDto(
    String slug,
    String categorySlug,
    Map<String, String> nameTranslations,
    Map<String, String> descriptionTranslations,
    BigDecimal price,
    BigDecimal discountPrice,
    String portion,
    String imageUrl,
    int sortOrder,
    boolean active,
    boolean available,
    List<ProductTag> tags,
    Instant createdAt
) {

}
