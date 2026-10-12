package com.onlinemenu.legal.controller;

import com.onlinemenu.common.dto.BaseResponse;
import com.onlinemenu.legal.dto.LegalDocumentAdminResponseDto;
import com.onlinemenu.legal.dto.LegalDocumentRequestDto;
import com.onlinemenu.legal.service.LegalDocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/legal")
@RequiredArgsConstructor
@PreAuthorize("hasRole(T(com.onlinemenu.common.constant.SecurityConstants).ROLE_SUPER_ADMIN)")
public class LegalDocumentController {

  private final LegalDocumentService legalDocumentService;

  @GetMapping
  public BaseResponse<List<LegalDocumentAdminResponseDto>> getAll() {
    return BaseResponse.success(legalDocumentService.getAll());
  }

  @PutMapping("/{slug}")
  public BaseResponse<LegalDocumentAdminResponseDto> update(@PathVariable String slug,
      @Valid @RequestBody LegalDocumentRequestDto dto) {
    return BaseResponse.success(legalDocumentService.update(slug, dto));
  }
}
