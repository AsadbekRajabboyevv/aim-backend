package com.onlinemenu.menu.dto;

import com.onlinemenu.cafe.domain.MenuTheme;
import com.onlinemenu.common.dto.DropdownDto;
import com.onlinemenu.menu.domain.ProductTag;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record PublicMenuResponseDto(
    PublicCafeDto cafe,
    List<DropdownDto> languages,
    String defaultLanguage,
    Map<String, Map<String, String>> labels,
    List<PublicCategoryDto> categories
) {

  public record PublicCafeDto(
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
      MenuTheme theme
  ) {

  }

  public record PublicCategoryDto(
      String slug,
      Map<String, String> nameTranslations,
      List<PublicProductDto> products
  ) {

  }

  public record PublicProductDto(
      String slug,
      Map<String, String> nameTranslations,
      Map<String, String> descriptionTranslations,
      BigDecimal price,
      BigDecimal discountPrice,
      String portion,
      String imageUrl,
      boolean available,
      List<ProductTag> tags
  ) {

  }
}
