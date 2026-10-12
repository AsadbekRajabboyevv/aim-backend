package com.onlinemenu.user.dto;

import com.onlinemenu.user.domain.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {

  @Mapping(target = "provider", ignore = true)
  User toEntity(UserRequestDto dto);

  UserResponseDto toResponseDto(User entity, long cafeCount);

  @Mapping(target = "provider", ignore = true)
  @Mapping(target = "slug", ignore = true)
  @Mapping(target = "password", ignore = true)
  void updateEntityFromDto(UserRequestDto dto, @MappingTarget User entity);
}
