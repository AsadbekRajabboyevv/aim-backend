package com.onlinemenu.systemmessage.dto;

import com.onlinemenu.systemmessage.domain.SystemMessage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SystemMessageMapper {

  SystemMessage toEntity(SystemMessageRequestDto dto);

  SystemMessageResponseDto toResponseDto(SystemMessage entity);

  @Mapping(target = "code", ignore = true)
  void updateEntityFromDto(SystemMessageRequestDto dto, @MappingTarget SystemMessage entity);
}
