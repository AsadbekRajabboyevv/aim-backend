package com.onlinemenu.cafe.service;

import com.onlinemenu.billing.service.EntitlementService;
import com.onlinemenu.billing.service.LimitKeys;
import com.onlinemenu.cafe.domain.Cafe;
import com.onlinemenu.cafe.domain.MenuTheme;
import com.onlinemenu.cafe.domain.SocialNetwork;
import com.onlinemenu.cafe.dto.CafeFilter;
import com.onlinemenu.cafe.dto.CafeMapper;
import com.onlinemenu.cafe.dto.CafeRequestDto;
import com.onlinemenu.cafe.dto.CafeResponseDto;
import com.onlinemenu.cafe.repository.CafeRepository;
import com.onlinemenu.common.constant.ExceptionMessageCodes;
import com.onlinemenu.common.dto.DropdownDto;
import com.onlinemenu.common.exception.BusinessException;
import com.onlinemenu.common.exception.ResourceNotFoundException;
import com.onlinemenu.common.util.ActiveLanguageProvider;
import com.onlinemenu.common.util.SlugUtils;
import com.onlinemenu.user.domain.Role;
import com.onlinemenu.user.domain.User;
import com.onlinemenu.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CafeServiceImpl implements CafeService {

  private static final Pattern HTTP_URL = Pattern.compile("^https?://\\S+$",
      Pattern.CASE_INSENSITIVE);
  private static final Set<String> SOCIAL_NETWORKS = Arrays.stream(SocialNetwork.values())
      .map(Enum::name)
      .collect(Collectors.toSet());

  private final CafeRepository cafeRepository;
  private final UserRepository userRepository;
  private final CafeMapper cafeMapper;
  private final ActiveLanguageProvider activeLanguageProvider;
  private final EntitlementService entitlementService;

  @Override
  public Page<CafeResponseDto> get(CafeFilter filter, Pageable pageable) {
    return cafeRepository.search(filter, pageable).map(cafeMapper::toResponseDto);
  }

  @Override
  public CafeResponseDto get(String slug) {
    return cafeMapper.toResponseDto(findBySlugOrThrow(slug));
  }

  @Override
  public List<DropdownDto> get() {
    return cafeRepository.findAll().stream()
        .map(cafe -> new DropdownDto(cafe.getSlug(), primaryName(cafe.getNameTranslations())))
        .toList();
  }

  @Override
  public List<CafeResponseDto> getByOwner(String ownerSlug) {
    return cafeRepository.findAllByOwnerSlugOrderByCreatedAtAsc(ownerSlug).stream()
        .map(cafeMapper::toResponseDto)
        .toList();
  }

  @Override
  @Transactional
  public CafeResponseDto create(CafeRequestDto dto, String ownerSlug) {
    Cafe cafe = cafeMapper.toEntity(dto);
    cafe.setSocialLinks(normalizeSocialLinks(dto.socialLinks()));
    cafe.setTheme(dto.theme() != null ? dto.theme() : MenuTheme.ESPRESSO);
    cafe.setOwner(resolveOwnerWithinLimit(ownerSlug));
    cafe.setSlug(SlugUtils.generateUniqueSlug(primaryName(dto.nameTranslations()),
        cafeRepository::existsBySlug));
    return cafeMapper.toResponseDto(cafeRepository.save(cafe));
  }

  @Override
  @Transactional
  public CafeResponseDto update(String slug, CafeRequestDto dto, String ownerSlug) {
    Cafe cafe = findBySlugOrThrow(slug);
    cafeMapper.updateEntityFromDto(dto, cafe);
    cafe.setSocialLinks(normalizeSocialLinks(dto.socialLinks()));
    if (dto.theme() != null) {
      cafe.setTheme(dto.theme());
    }
    String currentOwnerSlug = cafe.getOwner() != null ? cafe.getOwner().getSlug() : null;
    if (!Objects.equals(currentOwnerSlug, normalize(ownerSlug))) {
      cafe.setOwner(resolveOwnerWithinLimit(ownerSlug));
    }
    return cafeMapper.toResponseDto(cafe);
  }

  @Override
  @Transactional
  public void delete(String slug) {
    var cafe = findBySlugOrThrow(slug);
    cafe.setActive(false);
    cafeRepository.save(cafe);
  }

  private Cafe findBySlugOrThrow(String slug) {
    return cafeRepository.findBySlug(slug)
        .orElseThrow(
            () -> new ResourceNotFoundException(ExceptionMessageCodes.CAFE_NOT_FOUND, slug));
  }

  private User resolveOwnerWithinLimit(String ownerSlug) {
    String slug = normalize(ownerSlug);
    if (slug == null) {
      return null;
    }
    User owner = userRepository.findBySlugForUpdate(slug)
        .orElseThrow(
            () -> new ResourceNotFoundException(ExceptionMessageCodes.USER_NOT_FOUND, slug));
    if (owner.getRole() != Role.CAFE_ADMIN) {
      throw new BusinessException(ExceptionMessageCodes.CAFE_OWNER_INVALID, HttpStatus.BAD_REQUEST);
    }
    entitlementService.checkCount(owner, LimitKeys.CAFES);
    return owner;
  }

  private Map<String, String> normalizeSocialLinks(Map<String, String> links) {
    Map<String, String> result = new LinkedHashMap<>();
    if (links == null) {
      return result;
    }
    links.forEach((rawKey, rawUrl) -> {
      if (rawKey == null || rawUrl == null || rawUrl.isBlank()) {
        return;
      }
      String key = rawKey.trim().toUpperCase(Locale.ROOT);
      String url = rawUrl.trim();
      if (!SOCIAL_NETWORKS.contains(key) || !HTTP_URL.matcher(url).matches()) {
        throw new BusinessException(ExceptionMessageCodes.VALIDATION_URL_INVALID,
            HttpStatus.BAD_REQUEST, rawKey);
      }
      result.put(key, url);
    });
    return result;
  }

  private String normalize(String slug) {
    return StringUtils.hasText(slug) ? slug : null;
  }

  private String primaryName(Map<String, String> nameTranslations) {
    String defaultLanguageName = nameTranslations.get(
        activeLanguageProvider.getDefaultLanguageCode());
    if (defaultLanguageName != null) {
      return defaultLanguageName;
    }
    return nameTranslations.values().stream().findFirst().orElse("cafe");
  }
}
