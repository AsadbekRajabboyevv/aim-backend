package com.onlinemenu.file;

import com.onlinemenu.common.constant.FileConstants;
import lombok.Getter;

@Getter
public enum FileCategory {
  IMAGE(FileConstants.CATEGORY_IMAGE, FileConstants.ALLOWED_IMAGE_EXTENSIONS,
      FileConstants.MAX_IMAGE_SIZE_BYTES),
  VIDEO(FileConstants.CATEGORY_VIDEO, FileConstants.ALLOWED_VIDEO_EXTENSIONS,
      FileConstants.MAX_VIDEO_SIZE_BYTES);

  private final String directoryName;
  private final java.util.Set<String> allowedExtensions;
  private final long maxSizeBytes;

  FileCategory(String directoryName, java.util.Set<String> allowedExtensions, long maxSizeBytes) {
    this.directoryName = directoryName;
    this.allowedExtensions = allowedExtensions;
    this.maxSizeBytes = maxSizeBytes;
  }

}
