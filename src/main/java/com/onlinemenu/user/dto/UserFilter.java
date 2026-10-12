package com.onlinemenu.user.dto;

import com.onlinemenu.user.domain.Role;

public record UserFilter(
    String search,
    Role role,
    String cafeSlug,
    Boolean active
) {

}
