package com.onlinemenu.legal.dto;

import java.time.Instant;

/**
 * A legal document in one language, for the public pages.
 */
public record LegalDocumentResponseDto(
    String slug,
    String title,
    String content,
    String language,
    Instant updatedAt
) {

}
