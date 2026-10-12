package com.onlinemenu.ai.tool.controller;

import com.onlinemenu.ai.tool.dto.AiToolRequestDto;
import com.onlinemenu.ai.tool.dto.AiToolResponseDto;
import com.onlinemenu.ai.tool.service.AiToolService;
import com.onlinemenu.common.constant.AppConstants;
import com.onlinemenu.common.dto.BaseResponse;
import com.onlinemenu.common.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai/tools")
@RequiredArgsConstructor
@PreAuthorize("hasRole(T(com.onlinemenu.common.constant.SecurityConstants).ROLE_SUPER_ADMIN)")
public class AiToolController {

  private final AiToolService toolService;

  @GetMapping
  public BaseResponse<PageResponse<AiToolResponseDto>> get(
      @RequestParam(required = false) String search,
      @PageableDefault(size = AppConstants.DEFAULT_PAGE_SIZE, sort = "name") Pageable pageable) {
    return BaseResponse.success(PageResponse.from(toolService.get(search, pageable)));
  }

  @GetMapping("/{name}")
  public BaseResponse<AiToolResponseDto> get(@PathVariable String name) {
    return BaseResponse.success(toolService.get(name));
  }

  @PostMapping
  public BaseResponse<AiToolResponseDto> create(@Valid @RequestBody AiToolRequestDto dto) {
    return BaseResponse.success(toolService.create(dto));
  }

  @PutMapping("/{name}")
  public BaseResponse<AiToolResponseDto> update(@PathVariable String name,
      @Valid @RequestBody AiToolRequestDto dto) {
    return BaseResponse.success(toolService.update(name, dto));
  }

  @DeleteMapping("/{name}")
  public BaseResponse<Void> delete(@PathVariable String name) {
    toolService.delete(name);
    return BaseResponse.success(null);
  }
}
