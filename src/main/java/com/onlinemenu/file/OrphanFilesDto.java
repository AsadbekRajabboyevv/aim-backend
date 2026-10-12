package com.onlinemenu.file;

import java.util.List;

public record OrphanFilesDto(
    List<StoredFileDto> files,
    long totalSizeBytes,
    long gracePeriodMinutes
) {

}
