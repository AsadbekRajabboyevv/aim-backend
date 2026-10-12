package com.onlinemenu.common.constant;

import java.util.Set;

public final class FileConstants {

  private FileConstants() {
  }

  public static final String CATEGORY_IMAGE = "images";
  public static final String CATEGORY_VIDEO = "videos";

  public static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp",
      "gif");
  public static final Set<String> ALLOWED_VIDEO_EXTENSIONS = Set.of("mp4", "webm", "mov");

  public static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024 * 1024;
  public static final long MAX_VIDEO_SIZE_BYTES = 50L * 1024 * 1024;

  public static final String UPLOAD_URL_PREFIX = "/uploads";
}
