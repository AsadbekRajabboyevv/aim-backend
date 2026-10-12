package com.onlinemenu.security.oauth2;

import com.onlinemenu.common.util.SlugUtils;
import com.onlinemenu.security.UserPrincipal;
import com.onlinemenu.user.domain.AuthProvider;
import com.onlinemenu.user.domain.Role;
import com.onlinemenu.user.domain.User;
import com.onlinemenu.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

  private static final String ATTRIBUTE_EMAIL = "email";
  private static final String ATTRIBUTE_NAME = "name";

  private final UserRepository userRepository;

  @Override
  @Transactional
  public OAuth2User loadUser(OAuth2UserRequest userRequest) {
    OAuth2User oAuth2User = super.loadUser(userRequest);
    String email = oAuth2User.getAttribute(ATTRIBUTE_EMAIL);

    String name = oAuth2User.getAttribute(ATTRIBUTE_NAME);
    User user = userRepository.findByEmail(email).orElseGet(() -> register(email, name));

    if (user.getProvider() != AuthProvider.GOOGLE || !user.isEmailVerified()) {
      // Google has confirmed the address, so a pending password sign-up is verified too
      user.setProvider(AuthProvider.GOOGLE);
      user.setEmailVerified(true);
      userRepository.save(user);
    }

    return new OAuth2UserPrincipalAdapter(UserPrincipal.fromEntity(user),
        oAuth2User.getAttributes());
  }

  private User register(String email, String name) {
    String fullName = name != null && !name.isBlank() ? name : email;
    User user = User.builder()
        .fullName(fullName)
        .email(email)
        .role(Role.CAFE_ADMIN)
        .provider(AuthProvider.GOOGLE)
        .emailVerified(true)
        .active(true)
        .build();
    user.setSlug(SlugUtils.generateUniqueSlug(fullName, userRepository::existsBySlug));
    return userRepository.save(user);
  }
}
