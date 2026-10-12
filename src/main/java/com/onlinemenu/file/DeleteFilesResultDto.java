package com.onlinemenu.file;

import java.util.List;

public record DeleteFilesResultDto(
    int deletedCount,
    long freedBytes,
    List<String> skipped
) {

}
