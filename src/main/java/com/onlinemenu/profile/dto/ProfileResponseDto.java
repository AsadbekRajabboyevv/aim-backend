package com.onlinemenu.profile.dto;

import com.onlinemenu.user.domain.AuthProvider;
import com.onlinemenu.user.domain.Role;

import java.time.Instant;

public record ProfileResponseDto(
    String slug,
    String fullName,
    String email,
    Role role,
    AuthProvider provider,
    boolean hasPassword,
    String country,
    Instant createdAt
) {

}
