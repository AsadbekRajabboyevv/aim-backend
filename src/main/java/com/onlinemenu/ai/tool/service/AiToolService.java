package com.onlinemenu.ai.tool.service;

import com.onlinemenu.ai.tool.domain.AiTool;
import com.onlinemenu.ai.tool.dto.AiToolRequestDto;
import com.onlinemenu.ai.tool.dto.AiToolResponseDto;
import com.onlinemenu.user.domain.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AiToolService {

  Page<AiToolResponseDto> get(String search, Pageable pageable);

  AiToolResponseDto get(String name);

  AiToolResponseDto create(AiToolRequestDto dto);

  AiToolResponseDto update(String name, AiToolRequestDto dto);

  void delete(String name);

  List<AiTool> getAvailable(Role role);
}
