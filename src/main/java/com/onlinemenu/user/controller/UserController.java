package com.onlinemenu.user.controller;

import com.onlinemenu.common.constant.AppConstants;
import com.onlinemenu.common.dto.BaseResponse;
import com.onlinemenu.common.dto.DropdownDto;
import com.onlinemenu.common.dto.PageResponse;
import com.onlinemenu.user.domain.Role;
import com.onlinemenu.user.dto.UserFilter;
import com.onlinemenu.user.dto.UserRequestDto;
import com.onlinemenu.user.dto.UserResponseDto;
import com.onlinemenu.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole(T(com.onlinemenu.common.constant.SecurityConstants).ROLE_SUPER_ADMIN)")
public class UserController {

  private final UserService userService;

  @GetMapping
  public BaseResponse<PageResponse<UserResponseDto>> get(
      UserFilter filter,
      @PageableDefault(size = AppConstants.DEFAULT_PAGE_SIZE, sort = AppConstants.DEFAULT_SORT_FIELD) Pageable pageable) {
    return BaseResponse.success(PageResponse.from(userService.get(filter, pageable)));
  }

  @GetMapping("/dropdown")
  public BaseResponse<List<DropdownDto>> getDropdown(@RequestParam(required = false) Role role) {
    return BaseResponse.success(userService.get(role));
  }

  @GetMapping("/{slug}")
  public BaseResponse<UserResponseDto> get(@PathVariable String slug) {
    return BaseResponse.success(userService.get(slug));
  }

  @PostMapping
  public BaseResponse<UserResponseDto> create(@Valid @RequestBody UserRequestDto dto) {
    return BaseResponse.success(userService.create(dto));
  }

  @PutMapping("/{slug}")
  public BaseResponse<UserResponseDto> update(@PathVariable String slug,
      @Valid @RequestBody UserRequestDto dto) {
    return BaseResponse.success(userService.update(slug, dto));
  }

  @DeleteMapping("/{slug}")
  public BaseResponse<Void> delete(@PathVariable String slug) {
    userService.delete(slug);
    return BaseResponse.success(null);
  }
}
