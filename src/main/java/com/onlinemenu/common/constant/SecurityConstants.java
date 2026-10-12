package com.onlinemenu.common.constant;

public final class SecurityConstants {

    private SecurityConstants() {}

    public static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    public static final String ROLE_CAFE_ADMIN = "CAFE_ADMIN";

    public static final String AUTH_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String TOKEN_TYPE_ACCESS = "ACCESS";
    public static final String TOKEN_TYPE_REFRESH = "REFRESH";

    public static final String CLAIM_USER_SLUG = "slug";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_TOKEN_TYPE = "type";
    public static final String CLAIM_EMAIL = "email";

    public static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/**",
            "/api/v1/public/**",
            "/oauth2/**",
            "/login/oauth2/**",
            "/uploads/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/actuator/health"
    };
}
