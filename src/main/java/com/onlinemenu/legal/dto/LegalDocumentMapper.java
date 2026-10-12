package com.onlinemenu.legal.dto;

import com.onlinemenu.legal.domain.LegalDocument;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface LegalDocumentMapper {

  LegalDocumentAdminResponseDto toAdminResponseDto(LegalDocument entity);
}
