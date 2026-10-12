package com.onlinemenu.discount.dto;

import com.onlinemenu.discount.domain.Discount;
import com.onlinemenu.discount.domain.DiscountStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface DiscountMapper {

  @Mapping(target = "cafeSlug", source = "entity.cafe.slug")
  @Mapping(target = "cafeNameTranslations", source = "entity.cafe.nameTranslations")
  @Mapping(target = "productSlug", source = "entity.product.slug")
  @Mapping(target = "productNameTranslations", source = "entity.product.nameTranslations")
  @Mapping(target = "productImageUrl", source = "entity.product.imageUrl")
  @Mapping(target = "productPrice", source = "entity.product.price")
  DiscountResponseDto toResponseDto(Discount entity, DiscountStatus status, int percent);
}
