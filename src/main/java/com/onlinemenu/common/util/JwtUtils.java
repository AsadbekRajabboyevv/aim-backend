package com.onlinemenu.common.util;

import com.onlinemenu.common.constant.SecurityConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtils {

  @Value("${app.jwt.secret}")
  private String secret;

  @Getter
  @Value("${app.jwt.access-token-expiration-ms}")
  private long accessTokenExpirationMs;

  @Getter
  @Value("${app.jwt.refresh-token-expiration-ms}")
  private long refreshTokenExpirationMs;

  private SecretKey signingKey;

  @PostConstruct
  void init() {
    this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
  }

  public String generateAccessToken(JwtUserClaims claims) {
    return buildToken(claims, SecurityConstants.TOKEN_TYPE_ACCESS, accessTokenExpirationMs);
  }

  public String generateRefreshToken(JwtUserClaims claims) {
    return buildToken(claims, SecurityConstants.TOKEN_TYPE_REFRESH, refreshTokenExpirationMs);
  }

  private String buildToken(JwtUserClaims claims, String tokenType, long expirationMs) {
    Date now = new Date();
    Date expiry = new Date(now.getTime() + expirationMs);
    return Jwts.builder()
        .id(UUID.randomUUID().toString())
        .subject(claims.userId().toString())
        .claim(SecurityConstants.CLAIM_USER_SLUG, claims.slug())
        .claim(SecurityConstants.CLAIM_EMAIL, claims.email())
        .claim(SecurityConstants.CLAIM_ROLE, claims.role())
        .claim(SecurityConstants.CLAIM_TOKEN_TYPE, tokenType)
        .issuedAt(now)
        .expiration(expiry)
        .signWith(signingKey)
        .compact();
  }

  public Claims parseClaims(String token) {
    return Jwts.parser()
        .verifyWith(signingKey)
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  public boolean isValid(String token) {
    try {
      parseClaims(token);
      return true;
    } catch (JwtException | IllegalArgumentException ex) {
      return false;
    }
  }

  public JwtUserClaims extractUserClaims(String token) {
    Claims claims = parseClaims(token);
    return new JwtUserClaims(
        UUID.fromString(claims.getSubject()),
        claims.get(SecurityConstants.CLAIM_USER_SLUG, String.class),
        claims.get(SecurityConstants.CLAIM_EMAIL, String.class),
        claims.get(SecurityConstants.CLAIM_ROLE, String.class)
    );
  }

  public String extractTokenType(String token) {
    return parseClaims(token).get(SecurityConstants.CLAIM_TOKEN_TYPE, String.class);
  }
}
