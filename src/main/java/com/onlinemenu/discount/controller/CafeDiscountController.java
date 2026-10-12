package com.onlinemenu.discount.controller;

import com.onlinemenu.common.dto.BaseResponse;
import com.onlinemenu.discount.dto.DiscountRequestDto;
import com.onlinemenu.discount.dto.DiscountResponseDto;
import com.onlinemenu.discount.service.DiscountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cafes/{cafeSlug}/discounts")
@RequiredArgsConstructor
@PreAuthorize("""
    hasRole(T(com.onlinemenu.common.constant.SecurityConstants).ROLE_SUPER_ADMIN)
    or (hasRole(T(com.onlinemenu.common.constant.SecurityConstants).ROLE_CAFE_ADMIN)
        and @cafeSecurity.isOwner(#cafeSlug, authentication))
    """)
public class CafeDiscountController {

  private final DiscountService discountService;

  @GetMapping("/{slug}")
  public BaseResponse<DiscountResponseDto> get(@PathVariable String cafeSlug,
      @PathVariable String slug) {
    return BaseResponse.success(discountService.get(cafeSlug, slug));
  }

  @PostMapping
  public BaseResponse<DiscountResponseDto> create(@PathVariable String cafeSlug,
      @Valid @RequestBody DiscountRequestDto dto) {
    return BaseResponse.success(discountService.create(cafeSlug, dto));
  }

  @PutMapping("/{slug}")
  public BaseResponse<DiscountResponseDto> update(@PathVariable String cafeSlug,
      @PathVariable String slug,
      @Valid @RequestBody DiscountRequestDto dto) {
    return BaseResponse.success(discountService.update(cafeSlug, slug, dto));
  }

  @DeleteMapping("/{slug}")
  public BaseResponse<Void> delete(@PathVariable String cafeSlug, @PathVariable String slug) {
    discountService.delete(cafeSlug, slug);
    return BaseResponse.success(null);
  }
}
