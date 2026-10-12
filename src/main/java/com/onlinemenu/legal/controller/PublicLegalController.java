package com.onlinemenu.legal.controller;

import com.onlinemenu.common.dto.BaseResponse;
import com.onlinemenu.legal.dto.LegalDocumentResponseDto;
import com.onlinemenu.legal.service.LegalDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Terms of use and Privacy policy for the public pages, in the request language (X-Lang).
 */
@RestController
@RequestMapping("/api/v1/public/legal")
@RequiredArgsConstructor
public class PublicLegalController {

  private final LegalDocumentService legalDocumentService;

  @GetMapping("/{slug}")
  public BaseResponse<LegalDocumentResponseDto> get(@PathVariable String slug) {
    return BaseResponse.success(legalDocumentService.getPublic(slug));
  }
}
