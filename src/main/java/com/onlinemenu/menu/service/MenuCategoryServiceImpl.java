package com.onlinemenu.menu.service;

import com.onlinemenu.cafe.domain.Cafe;
import com.onlinemenu.common.constant.ExceptionMessageCodes;
import com.onlinemenu.common.exception.BusinessException;
import com.onlinemenu.common.exception.ResourceNotFoundException;
import com.onlinemenu.common.util.SlugUtils;
import com.onlinemenu.menu.domain.MenuCategory;
import com.onlinemenu.menu.dto.MenuCategoryRequestDto;
import com.onlinemenu.menu.dto.MenuCategoryResponseDto;
import com.onlinemenu.menu.dto.MenuMapper;
import com.onlinemenu.menu.repository.MenuCategoryRepository;
import com.onlinemenu.menu.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuCategoryServiceImpl implements MenuCategoryService {

  private final MenuCategoryRepository categoryRepository;
  private final ProductRepository productRepository;
  private final MenuMapper menuMapper;
  private final MenuSupport menuSupport;

  @Override
  public List<MenuCategoryResponseDto> get(String cafeSlug) {
    Cafe cafe = menuSupport.findCafeOrThrow(cafeSlug);
    return toResponseDtos(cafe,
        categoryRepository.findAllByCafeIdOrderBySortOrderAscCreatedAtAsc(cafe.getId()));
  }

  @Override
  public MenuCategoryResponseDto get(String cafeSlug, String slug) {
    Cafe cafe = menuSupport.findCafeOrThrow(cafeSlug);
    MenuCategory category = findOrThrow(cafe, slug);
    return menuMapper.toResponseDto(category,
        productCounts(cafe).getOrDefault(category.getId(), 0L));
  }

  @Override
  @Transactional
  public MenuCategoryResponseDto create(String cafeSlug, MenuCategoryRequestDto dto) {
    Cafe cafe = menuSupport.findCafeOrThrow(cafeSlug);
    MenuCategory category = menuMapper.toEntity(dto);
    category.setCafe(cafe);
    category.setSortOrder(categoryRepository.findMaxSortOrder(cafe.getId()) + 1);
    category.setSlug(SlugUtils.generateUniqueSlug(
        menuSupport.primaryName(dto.nameTranslations(), "category"),
        categoryRepository::existsBySlug));
    return menuMapper.toResponseDto(categoryRepository.save(category), 0);
  }

  @Override
  @Transactional
  public MenuCategoryResponseDto update(String cafeSlug, String slug, MenuCategoryRequestDto dto) {
    Cafe cafe = menuSupport.findCafeOrThrow(cafeSlug);
    MenuCategory category = findOrThrow(cafe, slug);
    menuMapper.updateEntityFromDto(dto, category);
    return menuMapper.toResponseDto(category,
        productCounts(cafe).getOrDefault(category.getId(), 0L));
  }

  @Override
  @Transactional
  public void delete(String cafeSlug, String slug) {
    Cafe cafe = menuSupport.findCafeOrThrow(cafeSlug);
    MenuCategory category = findOrThrow(cafe, slug);
    if (productRepository.existsByCategoryId(category.getId())) {
      throw new BusinessException(ExceptionMessageCodes.CATEGORY_NOT_EMPTY, HttpStatus.CONFLICT);
    }
    categoryRepository.delete(category);
  }

  /**
   * Categories missing from {@code slugs} keep their relative order after the listed ones.
   */
  @Override
  @Transactional
  public List<MenuCategoryResponseDto> reorder(String cafeSlug, List<String> slugs) {
    Cafe cafe = menuSupport.findCafeOrThrow(cafeSlug);
    List<MenuCategory> categories = categoryRepository.findAllByCafeIdOrderBySortOrderAscCreatedAtAsc(
        cafe.getId());
    for (MenuCategory category : categories) {
      int index = slugs.indexOf(category.getSlug());
      category.setSortOrder(index >= 0 ? index : slugs.size() + categories.indexOf(category));
    }
    categories.sort((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()));
    return toResponseDtos(cafe, categories);
  }

  private MenuCategory findOrThrow(Cafe cafe, String slug) {
    return categoryRepository.findBySlugAndCafeId(slug, cafe.getId())
        .orElseThrow(
            () -> new ResourceNotFoundException(ExceptionMessageCodes.CATEGORY_NOT_FOUND, slug));
  }

  private List<MenuCategoryResponseDto> toResponseDtos(Cafe cafe, List<MenuCategory> categories) {
    Map<UUID, Long> counts = productCounts(cafe);
    return categories.stream()
        .map(category -> menuMapper.toResponseDto(category,
            counts.getOrDefault(category.getId(), 0L)))
        .toList();
  }

  private Map<UUID, Long> productCounts(Cafe cafe) {
    Map<UUID, Long> counts = new HashMap<>();
    for (Object[] row : productRepository.countByCategoryForCafe(cafe.getId())) {
      counts.put((UUID) row[0], (Long) row[1]);
    }
    return counts;
  }
}
