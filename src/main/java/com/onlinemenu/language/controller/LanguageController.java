package com.onlinemenu.language.controller;

import com.onlinemenu.common.constant.AppConstants;
import com.onlinemenu.common.constant.SecurityConstants;
import com.onlinemenu.common.dto.BaseResponse;
import com.onlinemenu.common.dto.DropdownDto;
import com.onlinemenu.common.dto.PageResponse;
import com.onlinemenu.language.dto.LanguageFilter;
import com.onlinemenu.language.dto.LanguageRequestDto;
import com.onlinemenu.language.dto.LanguageResponseDto;
import com.onlinemenu.language.service.LanguageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/languages")
@RequiredArgsConstructor
public class LanguageController {

  private final LanguageService languageService;

  @GetMapping
  public BaseResponse<PageResponse<LanguageResponseDto>> get(
      LanguageFilter filter,
      @PageableDefault(size = AppConstants.DEFAULT_PAGE_SIZE, sort = AppConstants.DEFAULT_SORT_FIELD) Pageable pageable) {
    return BaseResponse.success(PageResponse.from(languageService.get(filter, pageable)));
  }

  @GetMapping("/dropdown")
  public BaseResponse<List<DropdownDto>> getDropdown() {
    return BaseResponse.success(languageService.get());
  }

  @GetMapping("/active")
  public BaseResponse<List<LanguageResponseDto>> getActive() {
    return BaseResponse.success(languageService.getActive());
  }

  @GetMapping("/{code}")
  public BaseResponse<LanguageResponseDto> get(@PathVariable String code) {
    return BaseResponse.success(languageService.get(code));
  }

  @PostMapping
  @PreAuthorize("hasRole(T(com.onlinemenu.common.constant.SecurityConstants).ROLE_SUPER_ADMIN)")
  public BaseResponse<LanguageResponseDto> create(@Valid @RequestBody LanguageRequestDto dto) {
    return BaseResponse.success(languageService.create(dto));
  }

  @PutMapping("/{code}")
  @PreAuthorize("hasRole(T(com.onlinemenu.common.constant.SecurityConstants).ROLE_SUPER_ADMIN)")
  public BaseResponse<LanguageResponseDto> update(@PathVariable String code,
      @Valid @RequestBody LanguageRequestDto dto) {
    return BaseResponse.success(languageService.update(code, dto));
  }

  @DeleteMapping("/{code}")
  @PreAuthorize("hasRole(T(com.onlinemenu.common.constant.SecurityConstants).ROLE_SUPER_ADMIN)")
  public BaseResponse<Void> delete(@PathVariable String code) {
    languageService.delete(code);
    return BaseResponse.success(null);
  }
}
