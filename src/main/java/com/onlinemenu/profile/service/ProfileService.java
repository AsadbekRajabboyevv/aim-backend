package com.onlinemenu.profile.service;

import com.onlinemenu.auth.dto.TokenResponseDto;
import com.onlinemenu.profile.dto.PasswordChangeRequestDto;
import com.onlinemenu.profile.dto.ProfileRequestDto;
import com.onlinemenu.profile.dto.ProfileResponseDto;

public interface ProfileService {

  ProfileResponseDto get(String userSlug);

  ProfileResponseDto update(String userSlug, ProfileRequestDto dto);

  TokenResponseDto changePassword(String userSlug, PasswordChangeRequestDto dto);
}
