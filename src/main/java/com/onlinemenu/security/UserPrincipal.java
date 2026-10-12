package com.onlinemenu.security;

import com.onlinemenu.common.util.JwtUserClaims;
import com.onlinemenu.user.domain.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Getter
public class UserPrincipal implements UserDetails {

  private final UUID id;
  private final String slug;
  private final String email;
  private final String password;
  private final String role;
  private final boolean active;

  public UserPrincipal(UUID id, String slug, String email, String password, String role,
      boolean active) {
    this.id = id;
    this.slug = slug;
    this.email = email;
    this.password = password;
    this.role = role;
    this.active = active;
  }

  public static UserPrincipal fromEntity(User user) {
    return new UserPrincipal(
        user.getId(),
        user.getSlug(),
        user.getEmail(),
        user.getPassword(),
        user.getRole().name(),
        user.isActive()
    );
  }

  public static UserPrincipal fromJwtClaims(JwtUserClaims claims) {
    return new UserPrincipal(claims.userId(), claims.slug(), claims.email(), null, claims.role(),
        true);
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + role));
  }

  @Override
  public String getUsername() {
    // Telegram accounts have no email
    return email != null ? email : slug;
  }

  @Override
  public boolean isEnabled() {
    return active;
  }
}
