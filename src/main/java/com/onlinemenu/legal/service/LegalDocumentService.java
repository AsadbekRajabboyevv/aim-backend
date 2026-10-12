package com.onlinemenu.legal.service;

import com.onlinemenu.legal.dto.LegalDocumentAdminResponseDto;
import com.onlinemenu.legal.dto.LegalDocumentRequestDto;
import com.onlinemenu.legal.dto.LegalDocumentResponseDto;

import java.util.List;

public interface LegalDocumentService {

  /** The document in the request language, falling back to the default language. */
  LegalDocumentResponseDto getPublic(String slug);

  List<LegalDocumentAdminResponseDto> getAll();

  LegalDocumentAdminResponseDto update(String slug, LegalDocumentRequestDto dto);
}
