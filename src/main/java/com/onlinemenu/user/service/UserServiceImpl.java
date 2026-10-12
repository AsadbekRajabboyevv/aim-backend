package com.onlinemenu.user.service;

import com.onlinemenu.auth.repository.RefreshTokenRepository;
import com.onlinemenu.common.constant.ExceptionMessageCodes;
import com.onlinemenu.common.dto.DropdownDto;
import com.onlinemenu.common.exception.DuplicateResourceException;
import com.onlinemenu.common.exception.ResourceNotFoundException;
import com.onlinemenu.common.util.SlugUtils;
import com.onlinemenu.user.domain.AuthProvider;
import com.onlinemenu.user.domain.Role;
import com.onlinemenu.user.domain.User;
import com.onlinemenu.user.dto.UserFilter;
import com.onlinemenu.user.dto.UserMapper;
import com.onlinemenu.user.dto.UserRequestDto;
import com.onlinemenu.user.dto.UserResponseDto;
import com.onlinemenu.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;

  @Override
  public Page<UserResponseDto> get(UserFilter filter, Pageable pageable) {
    return userRepository.search(filter, pageable);
  }

  @Override
  public UserResponseDto get(String slug) {
    return toResponseDto(findBySlugOrThrow(slug));
  }

  @Override
  public List<DropdownDto> get(Role role) {
    List<User> users = role != null ? userRepository.findAllByRoleOrderByFullNameAsc(role)
        : userRepository.findAll();
    return users.stream()
        .map(user -> new DropdownDto(user.getSlug(), user.getFullName()))
        .toList();
  }

  @Override
  @Transactional
  public UserResponseDto create(UserRequestDto dto) {
    if (userRepository.existsByEmail(dto.email())) {
      throw new DuplicateResourceException(ExceptionMessageCodes.USER_EMAIL_ALREADY_EXISTS,
          dto.email());
    }
    User user = userMapper.toEntity(dto);
    user.setSlug(SlugUtils.generateUniqueSlug(dto.fullName(), userRepository::existsBySlug));
    user.setProvider(AuthProvider.LOCAL);
    // Created by an admin, so there is nobody to confirm the address
    user.setEmailVerified(true);
    user.setPassword(passwordEncoder.encode(dto.password()));
    return userMapper.toResponseDto(userRepository.save(user), 0);
  }

  @Override
  @Transactional
  public UserResponseDto update(String slug, UserRequestDto dto) {
    User user = findBySlugOrThrow(slug);
    if (!dto.email().equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmail(
        dto.email())) {
      throw new DuplicateResourceException(ExceptionMessageCodes.USER_EMAIL_ALREADY_EXISTS,
          dto.email());
    }
    userMapper.updateEntityFromDto(dto, user);
    if (StringUtils.hasText(dto.password())) {
      user.setPassword(passwordEncoder.encode(dto.password()));
    }
    return toResponseDto(user);
  }

  @Override
  @Transactional
  public void delete(String slug) {
    User user = findBySlugOrThrow(slug);
    refreshTokenRepository.deleteAllByUserId(user.getId());
    userRepository.delete(user);
  }

  private UserResponseDto toResponseDto(User user) {
    return userMapper.toResponseDto(user, userRepository.countOwnedCafes(user.getId()));
  }

  private User findBySlugOrThrow(String slug) {
    return userRepository.findBySlug(slug)
        .orElseThrow(
            () -> new ResourceNotFoundException(ExceptionMessageCodes.USER_NOT_FOUND, slug));
  }
}
