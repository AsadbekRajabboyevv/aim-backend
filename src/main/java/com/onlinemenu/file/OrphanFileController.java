package com.onlinemenu.file;

import com.onlinemenu.common.dto.BaseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/files/orphans")
@RequiredArgsConstructor
@PreAuthorize("hasRole(T(com.onlinemenu.common.constant.SecurityConstants).ROLE_SUPER_ADMIN)")
public class OrphanFileController {

  private final OrphanFileService orphanFileService;

  @GetMapping
  public BaseResponse<OrphanFilesDto> get() {
    return BaseResponse.success(orphanFileService.findOrphans());
  }

  @PostMapping("/delete")
  public BaseResponse<DeleteFilesResultDto> delete(@Valid @RequestBody DeleteFilesRequestDto dto) {
    return BaseResponse.success(orphanFileService.delete(dto.urls()));
  }
}
