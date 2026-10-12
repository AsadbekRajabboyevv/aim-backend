package com.onlinemenu.menu.service;

import com.onlinemenu.menu.dto.PublicMenuResponseDto;

public interface PublicMenuService {

  PublicMenuResponseDto get(String cafeSlug);
}
