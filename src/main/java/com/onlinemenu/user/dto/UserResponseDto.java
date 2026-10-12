package com.onlinemenu.user.dto;

import com.onlinemenu.user.domain.AuthProvider;
import com.onlinemenu.user.domain.Role;

import java.time.Instant;

public record UserResponseDto(
    String slug,
    String fullName,
    String email,
    Role role,
    long cafeCount,
    AuthProvider provider,
    boolean active,
    Instant createdAt
) {

}
