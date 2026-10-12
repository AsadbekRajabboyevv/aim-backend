package com.onlinemenu.menu.service;

import com.onlinemenu.cafe.domain.Cafe;
import com.onlinemenu.common.constant.AppConstants;
import com.onlinemenu.common.constant.ExceptionMessageCodes;
import com.onlinemenu.common.exception.BusinessException;
import com.onlinemenu.discount.service.DiscountPricing;
import com.onlinemenu.language.service.LanguageService;
import com.onlinemenu.menu.domain.MenuCategory;
import com.onlinemenu.menu.domain.Product;
import com.onlinemenu.menu.dto.MenuMapper;
import com.onlinemenu.menu.dto.PublicMenuResponseDto;
import com.onlinemenu.menu.repository.MenuCategoryRepository;
import com.onlinemenu.menu.repository.ProductRepository;
import com.onlinemenu.systemmessage.service.SystemMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicMenuServiceImpl implements PublicMenuService {

    private final MenuCategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final LanguageService languageService;
    private final SystemMessageService systemMessageService;
    private final MenuMapper menuMapper;
    private final MenuSupport menuSupport;
    private final DiscountPricing discountPricing;

    @Override
    public PublicMenuResponseDto get(String cafeSlug) {
        Cafe cafe = menuSupport.findCafeOrThrow(cafeSlug);
        if (!cafe.isActive()) {
            throw new BusinessException(ExceptionMessageCodes.MENU_CAFE_INACTIVE, HttpStatus.NOT_FOUND);
        }

        List<MenuCategory> categories = categoryRepository.findAllByCafeIdAndActiveTrueOrderBySortOrderAscCreatedAtAsc(cafe.getId());
        Map<UUID, List<Product>> productsByCategory = categories.isEmpty()
                ? Map.of()
                : productRepository.findVisibleByCategoryIds(categories.stream().map(MenuCategory::getId).toList())
                        .stream()
                        .collect(Collectors.groupingBy(product -> product.getCategory().getId()));

        Map<UUID, BigDecimal> runningPrices = discountPricing.runningPrices(cafe);

        // Empty categories are left out so the tablet never shows a section with nothing in it.
        List<PublicMenuResponseDto.PublicCategoryDto> categoryDtos = categories.stream()
                .filter(category -> productsByCategory.containsKey(category.getId()))
                .map(category -> new PublicMenuResponseDto.PublicCategoryDto(
                        category.getSlug(),
                        category.getNameTranslations(),
                        productsByCategory.get(category.getId()).stream()
                                .map(product -> menuMapper.toPublicProduct(
                                        product, discountPricing.priceFor(product, runningPrices)))
                                .toList()))
                .toList();

        return new PublicMenuResponseDto(
                menuMapper.toPublicCafe(cafe),
                languageService.get(),
                menuSupport.defaultLanguageCode(),
                systemMessageService.getTranslationsByPrefix(AppConstants.MENU_UI_MESSAGE_PREFIX),
                categoryDtos);
    }
}
