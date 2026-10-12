package com.onlinemenu.billing.controller;

import com.onlinemenu.billing.domain.PaymentStatus;
import com.onlinemenu.billing.dto.BillingSettingsDto;
import com.onlinemenu.billing.dto.BillingSettingsRequestDto;
import com.onlinemenu.billing.dto.LimitDefinitionRequestDto;
import com.onlinemenu.billing.dto.LimitDefinitionResponseDto;
import com.onlinemenu.billing.dto.PaymentResponseDto;
import com.onlinemenu.billing.dto.PlanRequestDto;
import com.onlinemenu.billing.dto.PlanResponseDto;
import com.onlinemenu.billing.dto.SubscriptionAdminDto;
import com.onlinemenu.billing.dto.SubscriptionAssignRequestDto;
import com.onlinemenu.billing.service.BillingSettingsService;
import com.onlinemenu.billing.service.LimitDefinitionService;
import com.onlinemenu.billing.service.PaymentService;
import com.onlinemenu.billing.service.PlanService;
import com.onlinemenu.billing.service.SubscriptionService;
import com.onlinemenu.common.constant.AppConstants;
import com.onlinemenu.common.dto.BaseResponse;
import com.onlinemenu.common.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Plans, their limits, billing settings, subscriptions and payments for the super admin.
 */
@RestController
@RequestMapping("/api/v1/billing/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole(T(com.onlinemenu.common.constant.SecurityConstants).ROLE_SUPER_ADMIN)")
public class BillingAdminController {

  private final PlanService planService;
  private final LimitDefinitionService limitDefinitionService;
  private final BillingSettingsService settingsService;
  private final SubscriptionService subscriptionService;
  private final PaymentService paymentService;

  @GetMapping("/plans")
  public BaseResponse<List<PlanResponseDto>> plans() {
    return BaseResponse.success(planService.get());
  }

  @PostMapping("/plans")
  public BaseResponse<PlanResponseDto> createPlan(@Valid @RequestBody PlanRequestDto dto) {
    return BaseResponse.success(planService.create(dto));
  }

  @PutMapping("/plans/{slug}")
  public BaseResponse<PlanResponseDto> updatePlan(@PathVariable String slug,
      @Valid @RequestBody PlanRequestDto dto) {
    return BaseResponse.success(planService.update(slug, dto));
  }

  @DeleteMapping("/plans/{slug}")
  public BaseResponse<Void> deletePlan(@PathVariable String slug) {
    planService.delete(slug);
    return BaseResponse.success(null);
  }

  @GetMapping("/limits")
  public BaseResponse<List<LimitDefinitionResponseDto>> limits() {
    return BaseResponse.success(limitDefinitionService.get());
  }

  @PostMapping("/limits")
  public BaseResponse<LimitDefinitionResponseDto> createLimit(
      @Valid @RequestBody LimitDefinitionRequestDto dto) {
    return BaseResponse.success(limitDefinitionService.create(dto));
  }

  @PutMapping("/limits/{key}")
  public BaseResponse<LimitDefinitionResponseDto> updateLimit(@PathVariable String key,
      @Valid @RequestBody LimitDefinitionRequestDto dto) {
    return BaseResponse.success(limitDefinitionService.update(key, dto));
  }

  @DeleteMapping("/limits/{key}")
  public BaseResponse<Void> deleteLimit(@PathVariable String key) {
    limitDefinitionService.delete(key);
    return BaseResponse.success(null);
  }

  @GetMapping("/settings")
  public BaseResponse<BillingSettingsDto> settings() {
    return BaseResponse.success(settingsService.get());
  }

  @PutMapping("/settings")
  public BaseResponse<BillingSettingsDto> updateSettings(
      @Valid @RequestBody BillingSettingsRequestDto dto) {
    return BaseResponse.success(settingsService.update(dto));
  }

  @GetMapping("/subscriptions")
  public BaseResponse<PageResponse<SubscriptionAdminDto>> subscriptions(
      @RequestParam(required = false) String search,
      @PageableDefault(size = AppConstants.DEFAULT_PAGE_SIZE, sort = AppConstants.DEFAULT_SORT_FIELD,
          direction = Sort.Direction.DESC) Pageable pageable) {
    return BaseResponse.success(PageResponse.from(subscriptionService.search(search, pageable)));
  }

  @PutMapping("/subscriptions/{userSlug}")
  public BaseResponse<SubscriptionAdminDto> assign(@PathVariable String userSlug,
      @Valid @RequestBody SubscriptionAssignRequestDto dto) {
    return BaseResponse.success(subscriptionService.assign(userSlug, dto));
  }

  @GetMapping("/payments")
  public BaseResponse<PageResponse<PaymentResponseDto>> payments(
      @RequestParam(required = false) PaymentStatus status,
      @RequestParam(required = false) String provider,
      @PageableDefault(size = AppConstants.DEFAULT_PAGE_SIZE, sort = AppConstants.DEFAULT_SORT_FIELD,
          direction = Sort.Direction.DESC) Pageable pageable) {
    return BaseResponse.success(
        PageResponse.from(paymentService.search(status, provider, pageable)));
  }
}
