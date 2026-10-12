package com.onlinemenu.file;

import java.time.Instant;


public record StoredFileDto(
    String url,
    String cafeSlug,
    String category,
    String fileName,
    long sizeBytes,
    Instant lastModified,
    boolean recent
) {

}
