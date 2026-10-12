package com.onlinemenu.ai.tool.dto;

import com.onlinemenu.ai.tool.domain.AiTool;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AiToolMapper {

  AiTool toEntity(AiToolRequestDto dto);

  AiToolResponseDto toResponseDto(AiTool entity);

  void updateEntityFromDto(AiToolRequestDto dto, @MappingTarget AiTool entity);
}
