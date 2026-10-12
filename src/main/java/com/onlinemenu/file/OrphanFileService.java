package com.onlinemenu.file;

import com.onlinemenu.common.constant.FileConstants;
import com.onlinemenu.common.util.UploadReferenceProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrphanFileService {

  static final Duration GRACE_PERIOD = Duration.ofHours(1);

  private final List<UploadReferenceProvider> referenceProviders;

  @Value("${app.upload.base-dir}")
  private String baseDir;

  public OrphanFilesDto findOrphans() {
    List<StoredFileDto> orphans = new ArrayList<>(orphansByUrl().values());
    orphans.sort(Comparator.comparing(StoredFileDto::lastModified).reversed());
    long total = orphans.stream().mapToLong(StoredFileDto::sizeBytes).sum();
    return new OrphanFilesDto(orphans, total, GRACE_PERIOD.toMinutes());
  }

  public DeleteFilesResultDto delete(Collection<String> urls) {
    Map<String, StoredFileDto> orphans = orphansByUrl();
    Path root = root();
    int deleted = 0;
    long freed = 0;
    List<String> skipped = new ArrayList<>();

    for (String url : new HashSet<>(urls)) {
      StoredFileDto orphan = orphans.get(url);
      if (orphan == null || orphan.recent()) {
        skipped.add(url);
        continue;
      }
      Path file = root.resolve(url.substring(FileConstants.UPLOAD_URL_PREFIX.length() + 1))
          .normalize();
      if (!file.startsWith(root)) {
        skipped.add(url);
        continue;
      }
      try {
        if (Files.deleteIfExists(file)) {
          deleted++;
          freed += orphan.sizeBytes();
        } else {
          skipped.add(url);
        }
      } catch (IOException ex) {
        log.warn("Could not delete orphaned upload {}", file, ex);
        skipped.add(url);
      }
    }
    log.info("Deleted {} orphaned upload(s), {} bytes; skipped {}", deleted, freed, skipped.size());
    return new DeleteFilesResultDto(deleted, freed, skipped);
  }

  private Map<String, StoredFileDto> orphansByUrl() {
    Set<String> referenced = referencedPaths();
    Map<String, StoredFileDto> result = new LinkedHashMap<>();
    for (StoredFileDto file : storedFiles()) {
      if (!referenced.contains(file.url())) {
        result.put(file.url(), file);
      }
    }
    return result;
  }

  private Set<String> referencedPaths() {
    Set<String> paths = new HashSet<>();
    String marker = FileConstants.UPLOAD_URL_PREFIX + "/";
    for (UploadReferenceProvider provider : referenceProviders) {
      for (String url : provider.referencedUploadUrls()) {
        if (url == null) {
          continue;
        }
        int index = url.indexOf(marker);
        if (index >= 0) {
          String path = url.substring(index);
          int query = path.indexOf('?');
          paths.add(query >= 0 ? path.substring(0, query) : path);
        }
      }
    }
    return paths;
  }

  private List<StoredFileDto> storedFiles() {
    Path root = root();
    if (!Files.isDirectory(root)) {
      return List.of();
    }
    Instant recentAfter = Instant.now().minus(GRACE_PERIOD);
    try (Stream<Path> paths = Files.walk(root)) {
      return paths.filter(Files::isRegularFile)
          .map(path -> toDto(root, path, recentAfter))
          .toList();
    } catch (IOException ex) {
      throw new UncheckedIOException("Failed to scan upload directory " + root, ex);
    }
  }

  private StoredFileDto toDto(Path root, Path path, Instant recentAfter) {
    Path relative = root.relativize(path);
    String relativeUrl = relative.toString().replace('\\', '/');
    String cafeSlug = relative.getNameCount() > 1 ? relative.getName(0).toString() : null;
    String category = relative.getNameCount() > 2 ? relative.getName(1).toString() : null;
    try {
      BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
      Instant modified = attributes.lastModifiedTime().toInstant();
      return new StoredFileDto(FileConstants.UPLOAD_URL_PREFIX + "/" + relativeUrl, cafeSlug,
          category,
          path.getFileName().toString(), attributes.size(), modified,
          modified.isAfter(recentAfter));
    } catch (IOException ex) {
      throw new UncheckedIOException("Failed to read " + path, ex);
    }
  }

  private Path root() {
    return Path.of(baseDir).toAbsolutePath().normalize();
  }
}
