package com.onlinemenu.legal.dto;

import java.time.Instant;
import java.util.Map;

/**
 * A legal document with all its translations, for the super admin editor.
 */
public record LegalDocumentAdminResponseDto(
    String slug,
    Map<String, String> titleTranslations,
    Map<String, String> contentTranslations,
    Instant updatedAt,
    String updatedBy
) {

}
