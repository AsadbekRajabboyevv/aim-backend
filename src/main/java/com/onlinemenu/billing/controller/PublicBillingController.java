package com.onlinemenu.billing.controller;

import com.onlinemenu.billing.domain.Currency;
import com.onlinemenu.billing.dto.PlanCatalogDto;
import com.onlinemenu.billing.service.PlanService;
import com.onlinemenu.common.dto.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Plans for the landing page.
 */
@RestController
@RequestMapping("/api/v1/public/billing")
@RequiredArgsConstructor
public class PublicBillingController {

  private final PlanService planService;

  @GetMapping("/plans")
  public BaseResponse<PlanCatalogDto> plans(@RequestParam(required = false) String country) {
    return BaseResponse.success(planService.catalog(Currency.forCountry(country)));
  }
}
