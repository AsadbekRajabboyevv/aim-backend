package com.onlinemenu.file;

import com.onlinemenu.common.dto.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/cafes/{cafeSlug}/files")
@RequiredArgsConstructor
@PreAuthorize("""
    hasRole(T(com.onlinemenu.common.constant.SecurityConstants).ROLE_SUPER_ADMIN)
    or (hasRole(T(com.onlinemenu.common.constant.SecurityConstants).ROLE_CAFE_ADMIN)
        and @cafeSecurity.isOwner(#cafeSlug, authentication))
    """)
public class FileController {

  private final FileStorageService fileStorageService;

  @PostMapping
  public BaseResponse<FileUploadResponseDto> upload(
      @PathVariable String cafeSlug,
      @RequestParam FileCategory category,
      @RequestPart("file") MultipartFile file) {
    String url = fileStorageService.store(cafeSlug, file, category);
    return BaseResponse.success(new FileUploadResponseDto(url));
  }
}
