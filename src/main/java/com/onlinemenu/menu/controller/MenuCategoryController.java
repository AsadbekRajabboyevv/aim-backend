package com.onlinemenu.menu.controller;

import com.onlinemenu.common.dto.BaseResponse;
import com.onlinemenu.menu.dto.MenuCategoryRequestDto;
import com.onlinemenu.menu.dto.MenuCategoryResponseDto;
import com.onlinemenu.menu.dto.ReorderRequestDto;
import com.onlinemenu.menu.service.MenuCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cafes/{cafeSlug}/categories")
@RequiredArgsConstructor
@PreAuthorize("""
    hasRole(T(com.onlinemenu.common.constant.SecurityConstants).ROLE_SUPER_ADMIN)
    or (hasRole(T(com.onlinemenu.common.constant.SecurityConstants).ROLE_CAFE_ADMIN)
        and @cafeSecurity.isOwner(#cafeSlug, authentication))
    """)
public class MenuCategoryController {

  private final MenuCategoryService categoryService;

  @GetMapping
  public BaseResponse<List<MenuCategoryResponseDto>> get(@PathVariable String cafeSlug) {
    return BaseResponse.success(categoryService.get(cafeSlug));
  }

  @GetMapping("/{slug}")
  public BaseResponse<MenuCategoryResponseDto> get(@PathVariable String cafeSlug,
      @PathVariable String slug) {
    return BaseResponse.success(categoryService.get(cafeSlug, slug));
  }

  @PostMapping
  public BaseResponse<MenuCategoryResponseDto> create(@PathVariable String cafeSlug,
      @Valid @RequestBody MenuCategoryRequestDto dto) {
    return BaseResponse.success(categoryService.create(cafeSlug, dto));
  }

  @PutMapping("/order")
  public BaseResponse<List<MenuCategoryResponseDto>> reorder(@PathVariable String cafeSlug,
      @Valid @RequestBody ReorderRequestDto dto) {
    return BaseResponse.success(categoryService.reorder(cafeSlug, dto.slugs()));
  }

  @PutMapping("/{slug}")
  public BaseResponse<MenuCategoryResponseDto> update(@PathVariable String cafeSlug,
      @PathVariable String slug,
      @Valid @RequestBody MenuCategoryRequestDto dto) {
    return BaseResponse.success(categoryService.update(cafeSlug, slug, dto));
  }

  @DeleteMapping("/{slug}")
  public BaseResponse<Void> delete(@PathVariable String cafeSlug, @PathVariable String slug) {
    categoryService.delete(cafeSlug, slug);
    return BaseResponse.success(null);
  }
}
