package com.onlinemenu.cafe.controller;

import com.onlinemenu.billing.service.EntitlementService;
import com.onlinemenu.billing.service.LimitKeys;
import com.onlinemenu.cafe.dto.CafeFilter;
import com.onlinemenu.cafe.dto.CafeRequestDto;
import com.onlinemenu.cafe.dto.CafeResponseDto;
import com.onlinemenu.cafe.dto.MyCafesResponseDto;
import com.onlinemenu.cafe.service.CafeService;
import com.onlinemenu.common.constant.AppConstants;
import com.onlinemenu.common.constant.SecurityConstants;
import com.onlinemenu.common.dto.BaseResponse;
import com.onlinemenu.common.dto.DropdownDto;
import com.onlinemenu.common.dto.PageResponse;
import com.onlinemenu.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cafes")
@RequiredArgsConstructor
public class CafeController {

  private final CafeService cafeService;
  private final EntitlementService entitlementService;

  @GetMapping
  @PreAuthorize("hasRole(T(com.onlinemenu.common.constant.SecurityConstants).ROLE_SUPER_ADMIN)")
  public BaseResponse<PageResponse<CafeResponseDto>> get(
      CafeFilter filter,
      @PageableDefault(size = AppConstants.DEFAULT_PAGE_SIZE, sort = AppConstants.DEFAULT_SORT_FIELD) Pageable pageable) {
    return BaseResponse.success(PageResponse.from(cafeService.get(filter, pageable)));
  }

  @GetMapping("/dropdown")
  @PreAuthorize("hasRole(T(com.onlinemenu.common.constant.SecurityConstants).ROLE_SUPER_ADMIN)")
  public BaseResponse<List<DropdownDto>> getDropdown() {
    return BaseResponse.success(cafeService.get());
  }

  @GetMapping("/my")
  @PreAuthorize("hasRole(T(com.onlinemenu.common.constant.SecurityConstants).ROLE_CAFE_ADMIN)")
  public BaseResponse<MyCafesResponseDto> getMy(@AuthenticationPrincipal UserPrincipal principal) {
    return BaseResponse.success(new MyCafesResponseDto(
        cafeService.getByOwner(principal.getSlug()),
        entitlementService.limitFor(principal.getId(), LimitKeys.CAFES)));
  }

  @GetMapping("/{slug}")
  public BaseResponse<CafeResponseDto> get(@PathVariable String slug) {
    return BaseResponse.success(cafeService.get(slug));
  }

  @PostMapping
  @PreAuthorize("""
      hasRole(T(com.onlinemenu.common.constant.SecurityConstants).ROLE_SUPER_ADMIN)
      or hasRole(T(com.onlinemenu.common.constant.SecurityConstants).ROLE_CAFE_ADMIN)
      """)
  public BaseResponse<CafeResponseDto> create(@Valid @RequestBody CafeRequestDto dto,
      @AuthenticationPrincipal UserPrincipal principal) {
    return BaseResponse.success(cafeService.create(dto, resolveOwnerSlug(dto, principal)));
  }

  @PutMapping("/{slug}")
  @PreAuthorize("""
      hasRole(T(com.onlinemenu.common.constant.SecurityConstants).ROLE_SUPER_ADMIN)
      or (hasRole(T(com.onlinemenu.common.constant.SecurityConstants).ROLE_CAFE_ADMIN)
          and @cafeSecurity.isOwner(#slug, authentication))
      """)
  public BaseResponse<CafeResponseDto> update(@PathVariable String slug,
      @Valid @RequestBody CafeRequestDto dto,
      @AuthenticationPrincipal UserPrincipal principal) {
    return BaseResponse.success(cafeService.update(slug, dto, resolveOwnerSlug(dto, principal)));
  }

  @DeleteMapping("/{slug}")
  @PreAuthorize("""
      hasRole(T(com.onlinemenu.common.constant.SecurityConstants).ROLE_SUPER_ADMIN)
      or (hasRole(T(com.onlinemenu.common.constant.SecurityConstants).ROLE_CAFE_ADMIN)
          and @cafeSecurity.isOwner(#slug, authentication))
      """)
  public BaseResponse<Void> delete(@PathVariable String slug) {
    cafeService.delete(slug);
    return BaseResponse.success(null);
  }

  private String resolveOwnerSlug(CafeRequestDto dto, UserPrincipal principal) {
    return SecurityConstants.ROLE_SUPER_ADMIN.equals(principal.getRole()) ? dto.ownerSlug()
        : principal.getSlug();
  }
}
