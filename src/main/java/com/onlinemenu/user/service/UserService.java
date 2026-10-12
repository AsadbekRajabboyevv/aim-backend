package com.onlinemenu.user.service;

import com.onlinemenu.common.dto.DropdownDto;
import com.onlinemenu.user.domain.Role;
import com.onlinemenu.user.dto.UserFilter;
import com.onlinemenu.user.dto.UserRequestDto;
import com.onlinemenu.user.dto.UserResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService {

  Page<UserResponseDto> get(UserFilter filter, Pageable pageable);

  UserResponseDto get(String slug);

  List<DropdownDto> get(Role role);

  UserResponseDto create(UserRequestDto dto);

  UserResponseDto update(String slug, UserRequestDto dto);

  void delete(String slug);
}
