package com.onlinemenu.profile.controller;

import com.onlinemenu.auth.dto.TokenResponseDto;
import com.onlinemenu.common.dto.BaseResponse;
import com.onlinemenu.profile.dto.PasswordChangeRequestDto;
import com.onlinemenu.profile.dto.ProfileRequestDto;
import com.onlinemenu.profile.dto.ProfileResponseDto;
import com.onlinemenu.profile.service.ProfileService;
import com.onlinemenu.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ProfileController {

  private final ProfileService profileService;

  @GetMapping
  public BaseResponse<ProfileResponseDto> get(@AuthenticationPrincipal UserPrincipal principal) {
    return BaseResponse.success(profileService.get(principal.getSlug()));
  }

  @PutMapping
  public BaseResponse<ProfileResponseDto> update(@AuthenticationPrincipal UserPrincipal principal,
      @Valid @RequestBody ProfileRequestDto dto) {
    return BaseResponse.success(profileService.update(principal.getSlug(), dto));
  }

  @PutMapping("/password")
  public BaseResponse<TokenResponseDto> changePassword(
      @AuthenticationPrincipal UserPrincipal principal,
      @Valid @RequestBody PasswordChangeRequestDto dto) {
    return BaseResponse.success(profileService.changePassword(principal.getSlug(), dto));
  }
}
