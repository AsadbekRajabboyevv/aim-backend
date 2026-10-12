package com.onlinemenu.menu.controller;

import com.onlinemenu.common.dto.BaseResponse;
import com.onlinemenu.menu.dto.ProductAvailabilityRequestDto;
import com.onlinemenu.menu.dto.ProductBulkAvailabilityRequestDto;
import com.onlinemenu.menu.dto.ProductFilter;
import com.onlinemenu.menu.dto.ProductReorderRequestDto;
import com.onlinemenu.menu.dto.ProductRequestDto;
import com.onlinemenu.menu.dto.ProductResponseDto;
import com.onlinemenu.menu.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cafes/{cafeSlug}/products")
@RequiredArgsConstructor
@PreAuthorize("""
    hasRole(T(com.onlinemenu.common.constant.SecurityConstants).ROLE_SUPER_ADMIN)
    or (hasRole(T(com.onlinemenu.common.constant.SecurityConstants).ROLE_CAFE_ADMIN)
        and @cafeSecurity.isOwner(#cafeSlug, authentication))
    """)
public class ProductController {

  private final ProductService productService;

  @GetMapping
  public BaseResponse<List<ProductResponseDto>> get(@PathVariable String cafeSlug,
      ProductFilter filter) {
    return BaseResponse.success(productService.get(cafeSlug, filter));
  }

  @GetMapping("/{slug}")
  public BaseResponse<ProductResponseDto> get(@PathVariable String cafeSlug,
      @PathVariable String slug) {
    return BaseResponse.success(productService.get(cafeSlug, slug));
  }

  @PostMapping
  public BaseResponse<ProductResponseDto> create(@PathVariable String cafeSlug,
      @Valid @RequestBody ProductRequestDto dto) {
    return BaseResponse.success(productService.create(cafeSlug, dto));
  }

  @PutMapping("/order")
  public BaseResponse<List<ProductResponseDto>> reorder(@PathVariable String cafeSlug,
      @Valid @RequestBody ProductReorderRequestDto dto) {
    return BaseResponse.success(productService.reorder(cafeSlug, dto.categorySlug(), dto.slugs()));
  }

  @PatchMapping("/availability")
  public BaseResponse<Integer> updateAvailability(@PathVariable String cafeSlug,
      @RequestBody ProductBulkAvailabilityRequestDto dto) {
    return BaseResponse.success(
        productService.updateAvailability(cafeSlug, dto.slugs(), dto.available()));
  }

  @PutMapping("/{slug}")
  public BaseResponse<ProductResponseDto> update(@PathVariable String cafeSlug,
      @PathVariable String slug,
      @Valid @RequestBody ProductRequestDto dto) {
    return BaseResponse.success(productService.update(cafeSlug, slug, dto));
  }

  @PostMapping("/{slug}/copy")
  public BaseResponse<ProductResponseDto> copy(@PathVariable String cafeSlug,
      @PathVariable String slug) {
    return BaseResponse.success(productService.copy(cafeSlug, slug));
  }

  @PatchMapping("/{slug}/availability")
  public BaseResponse<ProductResponseDto> updateAvailability(@PathVariable String cafeSlug,
      @PathVariable String slug,
      @RequestBody ProductAvailabilityRequestDto dto) {
    return BaseResponse.success(productService.updateAvailability(cafeSlug, slug, dto.available()));
  }

  @DeleteMapping("/{slug}")
  public BaseResponse<Void> delete(@PathVariable String cafeSlug, @PathVariable String slug) {
    productService.delete(cafeSlug, slug);
    return BaseResponse.success(null);
  }
}
