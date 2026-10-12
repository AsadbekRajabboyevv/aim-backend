package com.onlinemenu.common.util;

import com.onlinemenu.common.constant.AppConstants;

import java.text.Normalizer;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public final class SlugUtils {

  private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
  private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

  private SlugUtils() {
  }

  public static String slugify(String input) {
    if (input == null || input.isBlank()) {
      return "";
    }
    String normalized = Normalizer.normalize(input, Normalizer.Form.NFKD);
    String withoutDiacritics = DIACRITICS.matcher(normalized).replaceAll("");
    String slug = NON_ALPHANUMERIC.matcher(withoutDiacritics.toLowerCase())
        .replaceAll(AppConstants.SLUG_SEPARATOR);
    return trimSeparators(slug);
  }

  public static String generateUniqueSlug(String base, Predicate<String> existsBySlug) {
    String candidate = slugify(base);
    if (candidate.isBlank()) {
      candidate = "item";
    }
    if (!existsBySlug.test(candidate)) {
      return candidate;
    }
    for (int attempt = 2; attempt <= AppConstants.SLUG_MAX_COLLISION_ATTEMPTS; attempt++) {
      String suffixed = candidate + AppConstants.SLUG_SEPARATOR + attempt;
      if (!existsBySlug.test(suffixed)) {
        return suffixed;
      }
    }
    throw new IllegalStateException("Unable to generate a unique slug for base: " + base);
  }

  private static String trimSeparators(String slug) {
    String result = slug;
    while (result.startsWith(AppConstants.SLUG_SEPARATOR)) {
      result = result.substring(1);
    }
    while (result.endsWith(AppConstants.SLUG_SEPARATOR)) {
      result = result.substring(0, result.length() - 1);
    }
    return result;
  }
}
