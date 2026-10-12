package com.onlinemenu.setting.dto;

import com.onlinemenu.common.constant.SettingKeys;
import com.onlinemenu.setting.domain.Setting;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, imports = SettingKeys.class)
public interface SettingMapper {

  @Mapping(target = "protectedKey", expression = "java(SettingKeys.PROTECTED.contains(entity.getKey()))")
  SettingResponseDto toResponseDto(Setting entity);
}
