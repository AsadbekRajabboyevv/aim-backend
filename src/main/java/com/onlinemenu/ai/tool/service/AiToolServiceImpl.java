package com.onlinemenu.ai.tool.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.onlinemenu.ai.tool.domain.AiTool;
import com.onlinemenu.ai.tool.dto.AiToolMapper;
import com.onlinemenu.ai.tool.dto.AiToolRequestDto;
import com.onlinemenu.ai.tool.dto.AiToolResponseDto;
import com.onlinemenu.ai.tool.repository.AiToolRepository;
import com.onlinemenu.common.constant.ExceptionMessageCodes;
import com.onlinemenu.common.exception.BusinessException;
import com.onlinemenu.common.exception.DuplicateResourceException;
import com.onlinemenu.common.exception.ResourceNotFoundException;
import com.onlinemenu.user.domain.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiToolServiceImpl implements AiToolService {

  private static final Pattern NAME = Pattern.compile("^[a-zA-Z0-9_-]{1,64}$");
  private static final Pattern PATH = Pattern.compile("^/api/v1/[A-Za-z0-9_\\-/{}.]*$");
  private static final List<String> BLOCKED_PATH_PREFIXES = List.of("/api/v1/ai", "/api/v1/auth",
      "/api/v1/profile/password");

  private final AiToolRepository toolRepository;
  private final AiToolMapper toolMapper;

  @Override
  public Page<AiToolResponseDto> get(String search, Pageable pageable) {
    return toolRepository.search(StringUtils.hasText(search) ? search.trim() : "", pageable)
        .map(toolMapper::toResponseDto);
  }

  @Override
  public AiToolResponseDto get(String name) {
    return toolMapper.toResponseDto(findOrThrow(name));
  }

  @Override
  @Transactional
  public AiToolResponseDto create(AiToolRequestDto dto) {
    validate(dto);
    if (toolRepository.existsByName(dto.name())) {
      throw new DuplicateResourceException(ExceptionMessageCodes.AI_TOOL_NAME_EXISTS, dto.name());
    }
    AiTool tool = toolMapper.toEntity(dto);
    tool.setAllowedRoles(new ArrayList<>(dto.allowedRoles().stream().distinct().sorted().toList()));
    return toolMapper.toResponseDto(toolRepository.save(tool));
  }

  @Override
  @Transactional
  public AiToolResponseDto update(String name, AiToolRequestDto dto) {
    validate(dto);
    AiTool tool = findOrThrow(name);
    if (!tool.getName().equals(dto.name()) && toolRepository.existsByName(dto.name())) {
      throw new DuplicateResourceException(ExceptionMessageCodes.AI_TOOL_NAME_EXISTS, dto.name());
    }
    toolMapper.updateEntityFromDto(dto, tool);
    tool.setAllowedRoles(new ArrayList<>(dto.allowedRoles().stream().distinct().sorted().toList()));
    return toolMapper.toResponseDto(tool);
  }

  @Override
  @Transactional
  public void delete(String name) {
    toolRepository.delete(findOrThrow(name));
  }

  @Override
  public List<AiTool> getAvailable(Role role) {
    return toolRepository.findAllByActiveTrueOrderByNameAsc().stream()
        .filter(tool -> tool.getAllowedRoles().contains(role))
        .toList();
  }

  private void validate(AiToolRequestDto dto) {
    if (!NAME.matcher(dto.name()).matches()) {
      throw invalid("name may contain only letters, digits, '_' and '-' (max 64)");
    }
    String path = dto.pathTemplate().trim();
    if (!PATH.matcher(path).matches() || path.contains("..")) {
      throw invalid(
          "path must start with /api/v1/ and contain only letters, digits, / _ - . and {placeholders}");
    }
    if (BLOCKED_PATH_PREFIXES.stream().anyMatch(path::startsWith)) {
      throw invalid("this API path cannot be used by a tool");
    }
    JsonNode schema = dto.inputSchema();
    if (!schema.isObject() || !"object".equals(schema.path("type").asText())
        || (schema.has("properties") && !schema.get("properties").isObject())) {
      throw invalid(
          "input schema must be a JSON object schema: {\"type\": \"object\", \"properties\": {...}}");
    }
    for (String placeholder : AiToolSchemas.placeholders(path)) {
      if (!schema.path("properties").has(placeholder)) {
        throw invalid(
            "path placeholder {" + placeholder + "} is not a property of the input schema");
      }
    }
  }

  private BusinessException invalid(String reason) {
    return new BusinessException(ExceptionMessageCodes.AI_TOOL_INVALID, HttpStatus.BAD_REQUEST,
        reason);
  }

  private AiTool findOrThrow(String name) {
    return toolRepository.findByName(name)
        .orElseThrow(
            () -> new ResourceNotFoundException(ExceptionMessageCodes.AI_TOOL_NOT_FOUND, name));
  }
}
