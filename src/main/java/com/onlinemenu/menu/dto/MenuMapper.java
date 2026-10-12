package com.onlinemenu.menu.dto;

import com.onlinemenu.cafe.domain.Cafe;
import com.onlinemenu.menu.domain.MenuCategory;
import com.onlinemenu.menu.domain.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import java.math.BigDecimal;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface MenuMapper {

  @Mapping(target = "cafe", ignore = true)
  @Mapping(target = "sortOrder", ignore = true)
  MenuCategory toEntity(MenuCategoryRequestDto dto);

  MenuCategoryResponseDto toResponseDto(MenuCategory entity, long productCount);

  @Mapping(target = "cafe", ignore = true)
  @Mapping(target = "slug", ignore = true)
  @Mapping(target = "sortOrder", ignore = true)
  void updateEntityFromDto(MenuCategoryRequestDto dto, @MappingTarget MenuCategory entity);

  @Mapping(target = "cafe", ignore = true)
  @Mapping(target = "category", ignore = true)
  @Mapping(target = "sortOrder", ignore = true)
  @Mapping(target = "tags", ignore = true)
  Product toEntity(ProductRequestDto dto);

  @Mapping(target = "categorySlug", source = "entity.category.slug")
  ProductResponseDto toResponseDto(Product entity, BigDecimal discountPrice);

  @Mapping(target = "cafe", ignore = true)
  @Mapping(target = "category", ignore = true)
  @Mapping(target = "slug", ignore = true)
  @Mapping(target = "sortOrder", ignore = true)
  @Mapping(target = "tags", ignore = true)
  void updateEntityFromDto(ProductRequestDto dto, @MappingTarget Product entity);

  PublicMenuResponseDto.PublicCafeDto toPublicCafe(Cafe cafe);

  PublicMenuResponseDto.PublicProductDto toPublicProduct(Product product, BigDecimal discountPrice);
}
