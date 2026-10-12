package com.onlinemenu.menu.service;

import com.onlinemenu.billing.service.EntitlementService;
import com.onlinemenu.billing.service.LimitKeys;
import com.onlinemenu.cafe.domain.Cafe;
import com.onlinemenu.common.constant.ExceptionMessageCodes;
import com.onlinemenu.common.exception.ResourceNotFoundException;
import com.onlinemenu.common.util.SlugUtils;
import com.onlinemenu.discount.service.DiscountPricing;
import com.onlinemenu.menu.domain.MenuCategory;
import com.onlinemenu.menu.domain.Product;
import com.onlinemenu.menu.domain.ProductTag;
import com.onlinemenu.menu.dto.MenuMapper;
import com.onlinemenu.menu.dto.ProductFilter;
import com.onlinemenu.menu.dto.ProductRequestDto;
import com.onlinemenu.menu.dto.ProductResponseDto;
import com.onlinemenu.menu.repository.MenuCategoryRepository;
import com.onlinemenu.menu.repository.ProductRepository;
import java.util.Comparator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

  private final ProductRepository productRepository;
  private final MenuCategoryRepository categoryRepository;
  private final MenuMapper menuMapper;
  private final MenuSupport menuSupport;
  private final DiscountPricing discountPricing;
  private final EntitlementService entitlementService;

  @Override
  public List<ProductResponseDto> get(String cafeSlug, ProductFilter filter) {
    Cafe cafe = menuSupport.findCafeOrThrow(cafeSlug);
    List<Product> products = StringUtils.hasText(filter.categorySlug())
        ? productRepository.findAllByCategoryIdOrderBySortOrderAscCreatedAtAsc(
        findCategoryOrThrow(cafe, filter.categorySlug()).getId())
        : productRepository.findAllForCafe(cafe.getId());
    return toResponseDtos(cafe, products);
  }

  @Override
  public ProductResponseDto get(String cafeSlug, String slug) {
    Cafe cafe = menuSupport.findCafeOrThrow(cafeSlug);
    return toResponseDto(cafe, findOrThrow(cafe, slug));
  }

  @Override
  @Transactional
  public ProductResponseDto create(String cafeSlug, ProductRequestDto dto) {
    Cafe cafe = menuSupport.findCafeOrThrow(cafeSlug);
    entitlementService.checkCount(cafe.getOwner(), LimitKeys.PRODUCTS);
    MenuCategory category = findCategoryOrThrow(cafe, dto.categorySlug());
    Product product = menuMapper.toEntity(dto);
    product.setCafe(cafe);
    product.setCategory(category);
    product.setTags(normalizeTags(dto.tags()));
    product.setSortOrder(productRepository.findMaxSortOrder(category.getId()) + 1);
    product.setSlug(SlugUtils.generateUniqueSlug(
        menuSupport.primaryName(dto.nameTranslations(), "product"),
        productRepository::existsBySlug));
    return menuMapper.toResponseDto(productRepository.save(product), null);
  }

  @Override
  @Transactional
  public ProductResponseDto update(String cafeSlug, String slug, ProductRequestDto dto) {
    Cafe cafe = menuSupport.findCafeOrThrow(cafeSlug);
    Product product = findOrThrow(cafe, slug);
    menuMapper.updateEntityFromDto(dto, product);
    product.setTags(normalizeTags(dto.tags()));
    if (!product.getCategory().getSlug().equals(dto.categorySlug())) {
      MenuCategory category = findCategoryOrThrow(cafe, dto.categorySlug());
      product.setCategory(category);
      product.setSortOrder(productRepository.findMaxSortOrder(category.getId()) + 1);
    }
    return toResponseDto(cafe, product);
  }

  @Override
  @Transactional
  public ProductResponseDto copy(String cafeSlug, String slug) {
    Cafe cafe = menuSupport.findCafeOrThrow(cafeSlug);
    entitlementService.checkCount(cafe.getOwner(), LimitKeys.PRODUCTS);
    Product original = findOrThrow(cafe, slug);
    productRepository.shiftSortOrdersAfter(original.getCategory().getId(), original.getSortOrder());
    Product copy = Product.builder()
        .cafe(cafe)
        .category(original.getCategory())
        .nameTranslations(new HashMap<>(original.getNameTranslations()))
        .descriptionTranslations(original.getDescriptionTranslations() == null
            ? new HashMap<>() : new HashMap<>(original.getDescriptionTranslations()))
        .price(original.getPrice())
        .portion(original.getPortion())
        .imageUrl(original.getImageUrl())
        .sortOrder(original.getSortOrder() + 1)
        .active(false)
        .available(original.isAvailable())
        .tags(new ArrayList<>(original.getTags()))
        .build();
    copy.setSlug(SlugUtils.generateUniqueSlug(
        menuSupport.primaryName(original.getNameTranslations(), "product"),
        productRepository::existsBySlug));
    return menuMapper.toResponseDto(productRepository.save(copy), null);
  }

  @Override
  @Transactional
  public List<ProductResponseDto> reorder(String cafeSlug, String categorySlug,
      List<String> slugs) {
    Cafe cafe = menuSupport.findCafeOrThrow(cafeSlug);
    MenuCategory category = findCategoryOrThrow(cafe, categorySlug);
    List<Product> products = new ArrayList<>(
        productRepository.findAllByCategoryIdOrderBySortOrderAscCreatedAtAsc(category.getId()));
    for (int position = 0; position < products.size(); position++) {
      Product product = products.get(position);
      int index = slugs.indexOf(product.getSlug());
      product.setSortOrder(index >= 0 ? index : slugs.size() + position);
    }
    products.sort(Comparator.comparingInt(Product::getSortOrder));
    return toResponseDtos(cafe, products);
  }

  @Override
  @Transactional
  public ProductResponseDto updateAvailability(String cafeSlug, String slug, boolean available) {
    Cafe cafe = menuSupport.findCafeOrThrow(cafeSlug);
    Product product = findOrThrow(cafe, slug);
    product.setAvailable(available);
    return toResponseDto(cafe, product);
  }

  @Override
  @Transactional
  public int updateAvailability(String cafeSlug, List<String> slugs, boolean available) {
    Cafe cafe = menuSupport.findCafeOrThrow(cafeSlug);
    Set<String> targets = slugs == null ? Set.of() : new HashSet<>(slugs);
    int changed = 0;
    for (Product product : productRepository.findAllForCafe(cafe.getId())) {
      if ((targets.isEmpty() || targets.contains(product.getSlug()))
          && product.isAvailable() != available) {
        product.setAvailable(available);
        changed++;
      }
    }
    return changed;
  }

  @Override
  @Transactional
  public void delete(String cafeSlug, String slug) {
    productRepository.delete(findOrThrow(menuSupport.findCafeOrThrow(cafeSlug), slug));
  }

  private List<ProductTag> normalizeTags(List<ProductTag> tags) {
    return tags == null ? new ArrayList<>()
        : new ArrayList<>(tags.stream().distinct().sorted().toList());
  }

  private List<ProductResponseDto> toResponseDtos(Cafe cafe, List<Product> products) {
    Map<UUID, BigDecimal> runningPrices = discountPricing.runningPrices(cafe);
    return products.stream()
        .map(product -> menuMapper.toResponseDto(product,
            discountPricing.priceFor(product, runningPrices)))
        .toList();
  }

  private ProductResponseDto toResponseDto(Cafe cafe, Product product) {
    return menuMapper.toResponseDto(product,
        discountPricing.priceFor(product, discountPricing.runningPrices(cafe)));
  }

  private Product findOrThrow(Cafe cafe, String slug) {
    return productRepository.findBySlugAndCafeId(slug, cafe.getId())
        .orElseThrow(
            () -> new ResourceNotFoundException(ExceptionMessageCodes.PRODUCT_NOT_FOUND, slug));
  }

  private MenuCategory findCategoryOrThrow(Cafe cafe, String categorySlug) {
    return categoryRepository.findBySlugAndCafeId(categorySlug, cafe.getId())
        .orElseThrow(() -> new ResourceNotFoundException(ExceptionMessageCodes.CATEGORY_NOT_FOUND,
            categorySlug));
  }
}
