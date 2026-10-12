package com.onlinemenu.profile.service;

import com.onlinemenu.auth.dto.TokenResponseDto;
import com.onlinemenu.auth.repository.RefreshTokenRepository;
import com.onlinemenu.auth.service.AuthService;
import com.onlinemenu.common.constant.ExceptionMessageCodes;
import com.onlinemenu.common.exception.BusinessException;
import com.onlinemenu.common.exception.ResourceNotFoundException;
import com.onlinemenu.profile.dto.PasswordChangeRequestDto;
import com.onlinemenu.profile.dto.ProfileRequestDto;
import com.onlinemenu.profile.dto.ProfileResponseDto;
import com.onlinemenu.user.domain.User;
import com.onlinemenu.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileServiceImpl implements ProfileService {

  private final UserRepository userRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthService authService;

  @Override
  public ProfileResponseDto get(String userSlug) {
    return toResponseDto(findOrThrow(userSlug));
  }

  @Override
  @Transactional
  public ProfileResponseDto update(String userSlug, ProfileRequestDto dto) {
    User user = findOrThrow(userSlug);
    user.setFullName(dto.fullName().trim());
    if (dto.country() != null) {
      user.setCountry(dto.country().trim().toUpperCase(Locale.ROOT));
    }
    return toResponseDto(user);
  }

  @Override
  @Transactional
  public TokenResponseDto changePassword(String userSlug, PasswordChangeRequestDto dto) {
    User user = findOrThrow(userSlug);
    if (user.getPassword() != null
        && (dto.currentPassword() == null || !passwordEncoder.matches(dto.currentPassword(),
        user.getPassword()))) {
      throw new BusinessException(ExceptionMessageCodes.PROFILE_PASSWORD_INVALID,
          HttpStatus.BAD_REQUEST);
    }
    user.setPassword(passwordEncoder.encode(dto.newPassword()));
    refreshTokenRepository.revokeAllByUserId(user.getId());
    return authService.issueTokensForSlug(user.getSlug());
  }

  private ProfileResponseDto toResponseDto(User user) {
    return new ProfileResponseDto(user.getSlug(), user.getFullName(), user.getEmail(),
        user.getRole(),
        user.getProvider(), user.getPassword() != null, user.getCountry(), user.getCreatedAt());
  }

  private User findOrThrow(String userSlug) {
    return userRepository.findBySlug(userSlug)
        .orElseThrow(
            () -> new ResourceNotFoundException(ExceptionMessageCodes.USER_NOT_FOUND, userSlug));
  }
}
