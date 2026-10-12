package com.onlinemenu.file;

import com.onlinemenu.common.constant.ExceptionMessageCodes;
import com.onlinemenu.common.constant.FileConstants;
import com.onlinemenu.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;


@Slf4j
@Service
public class FileStorageService {

  @Value("${app.upload.base-dir}")
  private String baseDir;

  public String store(String cafeSlug, MultipartFile file, FileCategory category) {
    validate(file, category);

    String extension = extractExtension(file.getOriginalFilename());
    String filename = UUID.randomUUID() + "." + extension;
    Path targetDir = Path.of(baseDir, cafeSlug, category.getDirectoryName());
    Path targetFile = targetDir.resolve(filename);

    try {
      Files.createDirectories(targetDir);
      try (InputStream inputStream = file.getInputStream()) {
        Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (IOException ex) {
      log.error("Failed to store uploaded file for cafe {}", cafeSlug, ex);
      throw new BusinessException(ExceptionMessageCodes.FILE_STORAGE_FAILED,
          HttpStatus.INTERNAL_SERVER_ERROR);
    }

    return FileConstants.UPLOAD_URL_PREFIX + "/" + cafeSlug + "/" + category.getDirectoryName()
        + "/" + filename;
  }

  private void validate(MultipartFile file, FileCategory category) {
    if (file == null || file.isEmpty()) {
      throw new BusinessException(ExceptionMessageCodes.FILE_EMPTY, HttpStatus.BAD_REQUEST);
    }
    if (file.getSize() > category.getMaxSizeBytes()) {
      throw new BusinessException(ExceptionMessageCodes.FILE_TOO_LARGE,
          HttpStatus.PAYLOAD_TOO_LARGE);
    }
    String extension = extractExtension(file.getOriginalFilename());
    if (!category.getAllowedExtensions().contains(extension)) {
      throw new BusinessException(ExceptionMessageCodes.FILE_TYPE_NOT_ALLOWED,
          HttpStatus.BAD_REQUEST, extension);
    }
  }

  private String extractExtension(String originalFilename) {
    if (!StringUtils.hasText(originalFilename) || !originalFilename.contains(".")) {
      throw new BusinessException(ExceptionMessageCodes.FILE_TYPE_NOT_ALLOWED,
          HttpStatus.BAD_REQUEST, originalFilename);
    }
    return originalFilename.substring(originalFilename.lastIndexOf('.') + 1)
        .toLowerCase(Locale.ROOT);
  }
}
