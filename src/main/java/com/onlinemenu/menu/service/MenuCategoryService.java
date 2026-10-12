package com.onlinemenu.menu.service;

import com.onlinemenu.menu.dto.MenuCategoryRequestDto;
import com.onlinemenu.menu.dto.MenuCategoryResponseDto;

import java.util.List;

public interface MenuCategoryService {

  List<MenuCategoryResponseDto> get(String cafeSlug);

  MenuCategoryResponseDto get(String cafeSlug, String slug);

  MenuCategoryResponseDto create(String cafeSlug, MenuCategoryRequestDto dto);

  MenuCategoryResponseDto update(String cafeSlug, String slug, MenuCategoryRequestDto dto);

  void delete(String cafeSlug, String slug);

  List<MenuCategoryResponseDto> reorder(String cafeSlug, List<String> slugs);
}
