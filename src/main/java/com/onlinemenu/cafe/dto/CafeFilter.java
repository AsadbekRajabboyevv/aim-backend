package com.onlinemenu.cafe.dto;

public record CafeFilter(
    String search,
    Boolean active,
    String ownerSlug
) {

}
