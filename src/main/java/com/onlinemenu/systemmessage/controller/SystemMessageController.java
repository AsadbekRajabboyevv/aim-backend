package com.onlinemenu.systemmessage.controller;

import com.onlinemenu.common.constant.AppConstants;
import com.onlinemenu.common.dto.BaseResponse;
import com.onlinemenu.common.dto.PageResponse;
import com.onlinemenu.systemmessage.dto.SystemMessageFilter;
import com.onlinemenu.systemmessage.dto.SystemMessageRequestDto;
import com.onlinemenu.systemmessage.dto.SystemMessageResponseDto;
import com.onlinemenu.systemmessage.service.SystemMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/system-messages")
@RequiredArgsConstructor
@PreAuthorize("hasRole(T(com.onlinemenu.common.constant.SecurityConstants).ROLE_SUPER_ADMIN)")
public class SystemMessageController {

  private final SystemMessageService systemMessageService;

  @GetMapping
  public BaseResponse<PageResponse<SystemMessageResponseDto>> get(
      SystemMessageFilter filter,
      // Native query underneath: sort keys must be column names.
      @PageableDefault(size = AppConstants.DEFAULT_PAGE_SIZE, sort = "code") Pageable pageable) {
    return BaseResponse.success(PageResponse.from(systemMessageService.get(filter, pageable)));
  }

  @GetMapping("/{code}")
  public BaseResponse<SystemMessageResponseDto> get(@PathVariable String code) {
    return BaseResponse.success(systemMessageService.get(code));
  }

  @PostMapping
  public BaseResponse<SystemMessageResponseDto> create(
      @Valid @RequestBody SystemMessageRequestDto dto) {
    return BaseResponse.success(systemMessageService.create(dto));
  }

  @PutMapping("/{code}")
  public BaseResponse<SystemMessageResponseDto> update(@PathVariable String code,
      @Valid @RequestBody SystemMessageRequestDto dto) {
    return BaseResponse.success(systemMessageService.update(code, dto));
  }

  @DeleteMapping("/{code}")
  public BaseResponse<Void> delete(@PathVariable String code) {
    systemMessageService.delete(code);
    return BaseResponse.success(null);
  }
}
