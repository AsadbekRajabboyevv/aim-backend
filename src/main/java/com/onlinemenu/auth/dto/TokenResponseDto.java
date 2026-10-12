package com.onlinemenu.auth.dto;

public record TokenResponseDto(
    String accessToken,
    String refreshToken,
    long expiresInMs,
    String tokenType
) {

}
