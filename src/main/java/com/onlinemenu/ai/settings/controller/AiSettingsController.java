package com.onlinemenu.ai.settings.controller;

import com.onlinemenu.ai.settings.dto.AiSettingsRequestDto;
import com.onlinemenu.ai.settings.dto.AiSettingsResponseDto;
import com.onlinemenu.ai.settings.service.AiSettingsService;
import com.onlinemenu.common.dto.BaseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai/settings")
@RequiredArgsConstructor
@PreAuthorize("hasRole(T(com.onlinemenu.common.constant.SecurityConstants).ROLE_SUPER_ADMIN)")
public class AiSettingsController {

  private final AiSettingsService settingsService;

  @GetMapping
  public BaseResponse<AiSettingsResponseDto> get() {
    return BaseResponse.success(settingsService.get());
  }

  @PutMapping
  public BaseResponse<AiSettingsResponseDto> update(@Valid @RequestBody AiSettingsRequestDto dto) {
    return BaseResponse.success(settingsService.update(dto));
  }

  @GetMapping("/models")
  public BaseResponse<List<String>> models(@RequestParam String provider) {
    return BaseResponse.success(settingsService.models(provider));
  }
}
