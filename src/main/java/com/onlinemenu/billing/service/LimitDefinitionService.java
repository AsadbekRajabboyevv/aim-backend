package com.onlinemenu.billing.service;

import com.onlinemenu.billing.domain.LimitDefinition;
import com.onlinemenu.billing.domain.LimitKind;
import com.onlinemenu.billing.domain.LimitPeriod;
import com.onlinemenu.billing.domain.Plan;
import com.onlinemenu.billing.dto.BillingMapper;
import com.onlinemenu.billing.dto.LimitDefinitionRequestDto;
import com.onlinemenu.billing.dto.LimitDefinitionResponseDto;
import com.onlinemenu.billing.repository.LimitDefinitionRepository;
import com.onlinemenu.billing.repository.PlanRepository;
import com.onlinemenu.billing.usage.UsageCounter;
import com.onlinemenu.common.constant.ExceptionMessageCodes;
import com.onlinemenu.common.exception.BusinessException;
import com.onlinemenu.common.exception.DuplicateResourceException;
import com.onlinemenu.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LimitDefinitionService {

  private static final Set<String> HTTP_METHODS = Set.of("GET", "POST", "PUT", "PATCH", "DELETE");
  private static final String API_PREFIX = "/api/v1/";
  /** Guarding these could lock users out of paying or signing in. */
  private static final List<String> UNGUARDED_PREFIXES = List.of("/api/v1/billing",
      "/api/v1/auth", "/api/v1/public", "/api/v1/profile");

  private final LimitDefinitionRepository definitionRepository;
  private final PlanRepository planRepository;
  private final BillingMapper mapper;
  private final List<UsageCounter> counters;

  private final AntPathMatcher pathMatcher = new AntPathMatcher();
  private final AtomicReference<List<LimitDefinition>> cache = new AtomicReference<>();

  /** All definitions, cached until one changes. The entities are detached; do not modify them. */
  public List<LimitDefinition> definitions() {
    List<LimitDefinition> cached = cache.get();
    if (cached == null) {
      cached = List.copyOf(definitionRepository.findAllByOrderBySortOrderAscKeyAsc());
      cache.set(cached);
    }
    return cached;
  }

  public Optional<LimitDefinition> find(String key) {
    return definitions().stream().filter(definition -> definition.getKey().equals(key))
        .findFirst();
  }

  /** Limits that apply to a request with this method and path. */
  public List<LimitDefinition> matching(String method, String path) {
    return definitions().stream()
        .filter(definition -> definition.getPathPattern() != null)
        .filter(definition -> definition.getHttpMethod() == null
            || definition.getHttpMethod().equalsIgnoreCase(method))
        .filter(definition -> pathMatcher.match(definition.getPathPattern(), path))
        .toList();
  }

  public boolean isEnforced(LimitDefinition definition) {
    return switch (definition.getKind()) {
      case COUNT -> counters.stream().anyMatch(counter -> counter.key().equals(definition.getKey()));
      case METERED, FEATURE -> definition.getPathPattern() != null
          || LimitKeys.CODE_ENFORCED.contains(definition.getKey());
    };
  }

  public List<LimitDefinitionResponseDto> get() {
    return definitions().stream().map(this::toDto).toList();
  }

  public LimitDefinitionResponseDto toDto(LimitDefinition definition) {
    return mapper.toDto(definition, isEnforced(definition));
  }

  @Transactional
  public LimitDefinitionResponseDto create(LimitDefinitionRequestDto dto) {
    if (!StringUtils.hasText(dto.key())) {
      throw invalid("key is required");
    }
    String key = dto.key().trim();
    if (definitionRepository.existsByKey(key)) {
      throw new DuplicateResourceException(ExceptionMessageCodes.BILLING_LIMIT_KEY_EXISTS, key);
    }
    LimitDefinition definition = LimitDefinition.builder().key(key).kind(dto.kind()).build();
    apply(definition, dto);
    LimitDefinition saved = definitionRepository.save(definition);
    cache.set(null);
    return toDto(saved);
  }

  @Transactional
  public LimitDefinitionResponseDto update(String key, LimitDefinitionRequestDto dto) {
    LimitDefinition definition = findOrThrow(key);
    if (definition.isBuiltIn() && (dto.kind() != definition.getKind()
        || (dto.period() != null && dto.period() != definition.getPeriod())
        || StringUtils.hasText(dto.pathPattern()))) {
      throw invalid("the kind, period and path of a built-in limit are fixed by the code");
    }
    if (dto.kind() != definition.getKind()) {
      definition.setKind(dto.kind());
    }
    apply(definition, dto);
    cache.set(null);
    return toDto(definition);
  }

  /** Also removes the limit from every plan. */
  @Transactional
  public void delete(String key) {
    LimitDefinition definition = findOrThrow(key);
    if (definition.isBuiltIn()) {
      throw new BusinessException(ExceptionMessageCodes.BILLING_LIMIT_SYSTEM, HttpStatus.CONFLICT);
    }
    for (Plan plan : planRepository.findAll()) {
      if (plan.getEntitlements().containsKey(key)) {
        Map<String, Long> entitlements = new HashMap<>(plan.getEntitlements());
        entitlements.remove(key);
        plan.setEntitlements(entitlements);
      }
    }
    definitionRepository.delete(definition);
    cache.set(null);
  }

  /** Keeps only known keys and valid values: -1 or more, and 0/1 for features. */
  public Map<String, Long> sanitize(Map<String, Long> entitlements) {
    if (entitlements == null) {
      return new HashMap<>();
    }
    Map<String, LimitDefinition> byKey = definitions().stream()
        .collect(Collectors.toMap(LimitDefinition::getKey, Function.identity()));
    Map<String, Long> result = new HashMap<>();
    entitlements.forEach((key, value) -> {
      LimitDefinition definition = byKey.get(key);
      if (definition == null || value == null) {
        return;
      }
      long normalized = definition.getKind() == LimitKind.FEATURE ? (value > 0 || value < 0 ? 1 : 0)
          : Math.max(value, Plan.UNLIMITED);
      result.put(key, normalized);
    });
    return result;
  }

  private void apply(LimitDefinition definition, LimitDefinitionRequestDto dto) {
    definition.setNameTranslations(cleanTranslations(dto.nameTranslations()));
    if (definition.getNameTranslations().isEmpty()) {
      throw invalid("a name is required");
    }
    LimitPeriod period = switch (definition.getKind()) {
      case METERED -> dto.period() != null ? dto.period() : LimitPeriod.MONTH;
      case COUNT, FEATURE -> LimitPeriod.NONE;
    };
    if (!definition.isBuiltIn()) {
      definition.setPeriod(period);
      String pattern = StringUtils.hasText(dto.pathPattern()) ? dto.pathPattern().trim() : null;
      String method = StringUtils.hasText(dto.httpMethod())
          ? dto.httpMethod().trim().toUpperCase(Locale.ROOT) : null;
      validatePattern(definition.getKind(), method, pattern);
      definition.setPathPattern(pattern);
      definition.setHttpMethod(pattern == null ? null : method);
    }
    long defaultValue = dto.defaultValue() == null ? Plan.UNLIMITED : dto.defaultValue();
    definition.setDefaultValue(definition.getKind() == LimitKind.FEATURE
        ? (defaultValue != 0 ? 1 : 0) : Math.max(defaultValue, Plan.UNLIMITED));
    if (dto.sortOrder() != null) {
      definition.setSortOrder(dto.sortOrder());
    }
  }

  private void validatePattern(LimitKind kind, String method, String pattern) {
    if (pattern == null) {
      if (method != null) {
        throw invalid("an HTTP method needs a path pattern");
      }
      return;
    }
    if (kind == LimitKind.COUNT) {
      throw invalid("COUNT limits are counted in code; use METERED or FEATURE for a path pattern");
    }
    if (!pattern.startsWith(API_PREFIX)
        || UNGUARDED_PREFIXES.stream().anyMatch(pattern::startsWith)) {
      throw invalid("the path must start with /api/v1/ and cannot guard billing, auth, profile or public endpoints");
    }
    if (method != null && !HTTP_METHODS.contains(method)) {
      throw invalid("unknown HTTP method " + method);
    }
  }

  private static Map<String, String> cleanTranslations(Map<String, String> translations) {
    Map<String, String> result = new HashMap<>();
    if (translations != null) {
      translations.forEach((code, text) -> {
        if (code != null && text != null && !text.isBlank()) {
          result.put(code, text.trim());
        }
      });
    }
    return result;
  }

  private LimitDefinition findOrThrow(String key) {
    return definitionRepository.findByKey(key)
        .orElseThrow(() -> new ResourceNotFoundException(
            ExceptionMessageCodes.BILLING_LIMIT_NOT_FOUND, key));
  }

  private static BusinessException invalid(String reason) {
    return new BusinessException(ExceptionMessageCodes.BILLING_LIMIT_INVALID,
        HttpStatus.BAD_REQUEST, reason);
  }
}
