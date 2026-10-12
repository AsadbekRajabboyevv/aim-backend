package com.onlinemenu.language.dto;

import com.onlinemenu.language.domain.Language;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface LanguageMapper {

  Language toEntity(LanguageRequestDto dto);

  // Lombok names the accessors of the "isActive"/"isDefault" fields isActive()/setActive() etc. (property "active"),
  // which MapStruct does not match to the records' isActive/isDefault components, so map them explicitly.
  @Mapping(target = "isActive", source = "active")
  @Mapping(target = "isDefault", source = "default")
  LanguageResponseDto toResponseDto(Language entity);

  @Mapping(target = "code", ignore = true)
  @Mapping(target = "active", source = "isActive")
  @Mapping(target = "default", source = "isDefault")
  void updateEntityFromDto(LanguageRequestDto dto, @MappingTarget Language entity);
}
