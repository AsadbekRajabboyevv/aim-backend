package com.onlinemenu.auth.dto;

/** Where the verification code went and when the user may ask for a new one. */
public record VerificationSentDto(
    String email,
    long expiresInSeconds,
    long resendAvailableInSeconds
) {

}
