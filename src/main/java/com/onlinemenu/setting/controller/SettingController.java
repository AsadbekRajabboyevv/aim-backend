package com.onlinemenu.setting.controller;

import com.onlinemenu.common.constant.AppConstants;
import com.onlinemenu.common.dto.BaseResponse;
import com.onlinemenu.common.dto.PageResponse;
import com.onlinemenu.setting.dto.SettingCreateRequestDto;
import com.onlinemenu.setting.dto.SettingFilter;
import com.onlinemenu.setting.dto.SettingRequestDto;
import com.onlinemenu.setting.dto.SettingResponseDto;
import com.onlinemenu.setting.service.SettingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
@PreAuthorize("hasRole(T(com.onlinemenu.common.constant.SecurityConstants).ROLE_SUPER_ADMIN)")
public class SettingController {

  private final SettingService settingService;

  @GetMapping
  public BaseResponse<PageResponse<SettingResponseDto>> get(
      SettingFilter filter,
      @PageableDefault(size = AppConstants.DEFAULT_PAGE_SIZE, sort = "key") Pageable pageable) {
    return BaseResponse.success(PageResponse.from(settingService.get(filter, pageable)));
  }

  @GetMapping("/{key}")
  public BaseResponse<SettingResponseDto> get(@PathVariable String key) {
    return BaseResponse.success(settingService.get(key));
  }

  @PostMapping
  public BaseResponse<SettingResponseDto> create(@Valid @RequestBody SettingCreateRequestDto dto) {
    return BaseResponse.success(settingService.create(dto));
  }

  @PutMapping("/{key}")
  public BaseResponse<SettingResponseDto> update(@PathVariable String key,
      @Valid @RequestBody SettingRequestDto dto) {
    return BaseResponse.success(settingService.update(key, dto));
  }

  @DeleteMapping("/{key}")
  public BaseResponse<Void> delete(@PathVariable String key) {
    settingService.delete(key);
    return BaseResponse.success(null);
  }
}
