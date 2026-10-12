package com.onlinemenu.common.util;

import java.util.UUID;

public record JwtUserClaims(
    UUID userId,
    String slug,
    String email,
    String role
) {

}
