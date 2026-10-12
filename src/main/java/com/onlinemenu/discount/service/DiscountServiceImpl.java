package com.onlinemenu.discount.service;

import com.onlinemenu.cafe.domain.Cafe;
import com.onlinemenu.cafe.repository.CafeRepository;
import com.onlinemenu.common.constant.ExceptionMessageCodes;
import com.onlinemenu.common.exception.BusinessException;
import com.onlinemenu.common.exception.ResourceNotFoundException;
import com.onlinemenu.common.util.SlugUtils;
import com.onlinemenu.discount.domain.Discount;
import com.onlinemenu.discount.domain.DiscountStatus;
import com.onlinemenu.discount.dto.DiscountFilter;
import com.onlinemenu.discount.dto.DiscountMapper;
import com.onlinemenu.discount.dto.DiscountRequestDto;
import com.onlinemenu.discount.dto.DiscountResponseDto;
import com.onlinemenu.discount.repository.DiscountRepository;
import com.onlinemenu.menu.domain.Product;
import com.onlinemenu.menu.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiscountServiceImpl implements DiscountService {

  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
  private static final String NO_FILTER = "";

  private final DiscountRepository discountRepository;
  private final CafeRepository cafeRepository;
  private final ProductRepository productRepository;
  private final DiscountMapper discountMapper;

  @Override
  public Page<DiscountResponseDto> get(DiscountFilter filter, String ownerSlug, Pageable pageable) {
    Instant now = Instant.now();
    return discountRepository.search(
            StringUtils.hasText(filter.search()) ? filter.search().trim() : NO_FILTER,
            StringUtils.hasText(filter.cafeSlug()) ? filter.cafeSlug() : NO_FILTER,
            ownerSlug != null ? ownerSlug : NO_FILTER,
            filter.status() != null ? filter.status().name() : NO_FILTER,
            now,
            pageable)
        .map(discount -> toResponseDto(discount, now));
  }

  @Override
  public DiscountResponseDto get(String cafeSlug, String slug) {
    return toResponseDto(findOrThrow(findCafeOrThrow(cafeSlug), slug), Instant.now());
  }

  @Override
  @Transactional
  public DiscountResponseDto create(String cafeSlug, DiscountRequestDto dto) {
    Cafe cafe = findCafeOrThrow(cafeSlug);
    Product product = findProductOrThrow(cafe, dto.productSlug());
    Discount discount = Discount.builder().cafe(cafe).build();
    apply(discount, product, dto);
    discount.setSlug(SlugUtils.generateUniqueSlug(product.getSlug() + "-discount",
        discountRepository::existsBySlug));
    return toResponseDto(discountRepository.save(discount), Instant.now());
  }

  @Override
  @Transactional
  public DiscountResponseDto update(String cafeSlug, String slug, DiscountRequestDto dto) {
    Cafe cafe = findCafeOrThrow(cafeSlug);
    Discount discount = findOrThrow(cafe, slug);
    Product product = discount.getProduct().getSlug().equals(dto.productSlug())
        ? discount.getProduct()
        : findProductOrThrow(cafe, dto.productSlug());
    apply(discount, product, dto);
    return toResponseDto(discount, Instant.now());
  }

  @Override
  @Transactional
  public void delete(String cafeSlug, String slug) {
    discountRepository.delete(findOrThrow(findCafeOrThrow(cafeSlug), slug));
  }

  private void apply(Discount discount, Product product, DiscountRequestDto dto) {
    if (dto.discountPrice().compareTo(product.getPrice()) >= 0) {
      throw new BusinessException(ExceptionMessageCodes.DISCOUNT_PRICE_INVALID,
          HttpStatus.BAD_REQUEST);
    }
    if (dto.startsAt() != null && dto.endsAt() != null && !dto.endsAt().isAfter(dto.startsAt())) {
      throw new BusinessException(ExceptionMessageCodes.DISCOUNT_PERIOD_INVALID,
          HttpStatus.BAD_REQUEST);
    }
    if (dto.active() && overlapsAnother(discount, product, dto.startsAt(), dto.endsAt())) {
      throw new BusinessException(ExceptionMessageCodes.DISCOUNT_OVERLAP, HttpStatus.CONFLICT);
    }
    discount.setProduct(product);
    discount.setDiscountPrice(dto.discountPrice());
    discount.setStartsAt(dto.startsAt());
    discount.setEndsAt(dto.endsAt());
    discount.setActive(dto.active());
  }

  private boolean overlapsAnother(Discount self, Product product, Instant startsAt,
      Instant endsAt) {
    return discountRepository.findAllByProductIdAndActiveTrue(product.getId()).stream()
        .filter(other -> self.getId() == null || !other.getId().equals(self.getId()))
        .anyMatch(other ->
            (other.getStartsAt() == null || endsAt == null || other.getStartsAt().isBefore(endsAt))
                && (other.getEndsAt() == null || startsAt == null || other.getEndsAt()
                .isAfter(startsAt)));
  }

  private DiscountResponseDto toResponseDto(Discount discount, Instant now) {
    return discountMapper.toResponseDto(discount, DiscountStatus.of(discount, now),
        percent(discount));
  }

  private int percent(Discount discount) {
    BigDecimal price = discount.getProduct().getPrice();
    if (price.signum() <= 0) {
      return 0;
    }
    return price.subtract(discount.getDiscountPrice())
        .multiply(HUNDRED)
        .divide(price, 0, RoundingMode.HALF_UP)
        .max(BigDecimal.ZERO)
        .intValue();
  }

  private Cafe findCafeOrThrow(String cafeSlug) {
    return cafeRepository.findBySlug(cafeSlug)
        .orElseThrow(
            () -> new ResourceNotFoundException(ExceptionMessageCodes.CAFE_NOT_FOUND, cafeSlug));
  }

  private Product findProductOrThrow(Cafe cafe, String productSlug) {
    return productRepository.findBySlugAndCafeId(productSlug, cafe.getId())
        .orElseThrow(() -> new ResourceNotFoundException(ExceptionMessageCodes.PRODUCT_NOT_FOUND,
            productSlug));
  }

  private Discount findOrThrow(Cafe cafe, String slug) {
    return discountRepository.findBySlugAndCafeId(slug, cafe.getId())
        .orElseThrow(
            () -> new ResourceNotFoundException(ExceptionMessageCodes.DISCOUNT_NOT_FOUND, slug));
  }
}
