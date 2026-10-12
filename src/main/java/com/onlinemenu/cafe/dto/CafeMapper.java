package com.onlinemenu.cafe.dto;

import com.onlinemenu.cafe.domain.Cafe;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CafeMapper {

  @Mapping(target = "owner", ignore = true)
  @Mapping(target = "socialLinks", ignore = true)
  Cafe toEntity(CafeRequestDto dto);

  @Mapping(target = "ownerSlug", source = "owner.slug")
  @Mapping(target = "ownerFullName", source = "owner.fullName")
  CafeResponseDto toResponseDto(Cafe entity);

  @Mapping(target = "slug", ignore = true)
  @Mapping(target = "owner", ignore = true)
  @Mapping(target = "socialLinks", ignore = true)
  @Mapping(target = "theme", ignore = true)
  void updateEntityFromDto(CafeRequestDto dto, @MappingTarget Cafe entity);
}
