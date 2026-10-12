package com.onlinemenu.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public final class HashUtils {

  private HashUtils() {
  }

  /** Base64 SHA-256 of a secret, for storing tokens and codes without keeping them readable. */
  public static String sha256(String value) {
    return Base64.getEncoder().encodeToString(sha256Bytes(value.getBytes(StandardCharsets.UTF_8)));
  }

  public static byte[] sha256Bytes(byte[] value) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(value);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 not available", ex);
    }
  }
}
