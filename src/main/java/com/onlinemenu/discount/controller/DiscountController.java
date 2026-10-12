package com.onlinemenu.discount.controller;

import com.onlinemenu.common.constant.AppConstants;
import com.onlinemenu.common.constant.SecurityConstants;
import com.onlinemenu.common.dto.BaseResponse;
import com.onlinemenu.common.dto.PageResponse;
import com.onlinemenu.discount.dto.DiscountFilter;
import com.onlinemenu.discount.dto.DiscountResponseDto;
import com.onlinemenu.discount.service.DiscountService;
import com.onlinemenu.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/discounts")
@RequiredArgsConstructor
@PreAuthorize("""
    hasRole(T(com.onlinemenu.common.constant.SecurityConstants).ROLE_SUPER_ADMIN)
    or hasRole(T(com.onlinemenu.common.constant.SecurityConstants).ROLE_CAFE_ADMIN)
    """)
public class DiscountController {

  private final DiscountService discountService;

  @GetMapping
  public BaseResponse<PageResponse<DiscountResponseDto>> get(
      DiscountFilter filter,
      @PageableDefault(size = AppConstants.DEFAULT_PAGE_SIZE, sort = AppConstants.DEFAULT_SORT_FIELD,
          direction = Sort.Direction.DESC) Pageable pageable,
      @AuthenticationPrincipal UserPrincipal principal) {
    String ownerSlug =
        SecurityConstants.ROLE_SUPER_ADMIN.equals(principal.getRole()) ? null : principal.getSlug();
    return BaseResponse.success(
        PageResponse.from(discountService.get(filter, ownerSlug, pageable)));
  }
}
