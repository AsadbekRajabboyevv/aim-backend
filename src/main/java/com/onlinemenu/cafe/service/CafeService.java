package com.onlinemenu.cafe.service;

import com.onlinemenu.cafe.dto.CafeFilter;
import com.onlinemenu.cafe.dto.CafeRequestDto;
import com.onlinemenu.cafe.dto.CafeResponseDto;
import com.onlinemenu.common.dto.DropdownDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CafeService {

  Page<CafeResponseDto> get(CafeFilter filter, Pageable pageable);

  CafeResponseDto get(String slug);

  List<DropdownDto> get();

  List<CafeResponseDto> getByOwner(String ownerSlug);

  CafeResponseDto create(CafeRequestDto dto, String ownerSlug);

  CafeResponseDto update(String slug, CafeRequestDto dto, String ownerSlug);

  void delete(String slug);
}
