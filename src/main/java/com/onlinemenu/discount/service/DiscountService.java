package com.onlinemenu.discount.service;

import com.onlinemenu.discount.dto.DiscountFilter;
import com.onlinemenu.discount.dto.DiscountRequestDto;
import com.onlinemenu.discount.dto.DiscountResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DiscountService {

  Page<DiscountResponseDto> get(DiscountFilter filter, String ownerSlug, Pageable pageable);

  DiscountResponseDto get(String cafeSlug, String slug);

  DiscountResponseDto create(String cafeSlug, DiscountRequestDto dto);

  DiscountResponseDto update(String cafeSlug, String slug, DiscountRequestDto dto);

  void delete(String cafeSlug, String slug);
}
