package com.onlinemenu.security;

import com.onlinemenu.cafe.repository.CafeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("cafeSecurity")
@RequiredArgsConstructor
public class CafeSecurity {

  private final CafeRepository cafeRepository;

  public boolean isOwner(String cafeSlug, Authentication authentication) {
    if (cafeSlug == null || authentication == null
        || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
      return false;
    }
    return cafeRepository.existsBySlugAndOwnerSlug(cafeSlug, principal.getSlug());
  }
}
