package com.onlinemenu.security.oauth2;

import com.onlinemenu.security.UserPrincipal;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

@Getter
public class OAuth2UserPrincipalAdapter implements OAuth2User {

  private final UserPrincipal userPrincipal;
  private final Map<String, Object> attributes;

  public OAuth2UserPrincipalAdapter(UserPrincipal userPrincipal, Map<String, Object> attributes) {
    this.userPrincipal = userPrincipal;
    this.attributes = attributes;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return userPrincipal.getAuthorities();
  }

  @Override
  public String getName() {
    return userPrincipal.getSlug();
  }
}
