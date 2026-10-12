package com.onlinemenu.menu.service;

import com.onlinemenu.menu.dto.ProductFilter;
import com.onlinemenu.menu.dto.ProductRequestDto;
import com.onlinemenu.menu.dto.ProductResponseDto;

import java.util.List;

public interface ProductService {

  List<ProductResponseDto> get(String cafeSlug, ProductFilter filter);

  ProductResponseDto get(String cafeSlug, String slug);

  ProductResponseDto create(String cafeSlug, ProductRequestDto dto);

  ProductResponseDto update(String cafeSlug, String slug, ProductRequestDto dto);

  ProductResponseDto copy(String cafeSlug, String slug);

  List<ProductResponseDto> reorder(String cafeSlug, String categorySlug, List<String> slugs);

  ProductResponseDto updateAvailability(String cafeSlug, String slug, boolean available);

  int updateAvailability(String cafeSlug, List<String> slugs, boolean available);

  void delete(String cafeSlug, String slug);
}
