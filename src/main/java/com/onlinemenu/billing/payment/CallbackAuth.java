package com.onlinemenu.billing.payment;

import jakarta.servlet.http.HttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Helpers for checking payment system requests.
 */
public final class CallbackAuth {

  private CallbackAuth() {
  }

  /** Whether the request carries HTTP Basic credentials equal to these (login null = any). */
  public static boolean basicMatches(HttpServletRequest request, String login, String password) {
    String header = request.getHeader("Authorization");
    if (header == null || !header.regionMatches(true, 0, "Basic ", 0, 6) || password == null) {
      return false;
    }
    String decoded;
    try {
      decoded = new String(Base64.getDecoder().decode(header.substring(6).trim()),
          StandardCharsets.UTF_8);
    } catch (IllegalArgumentException ex) {
      return false;
    }
    int colon = decoded.indexOf(':');
    if (colon < 0) {
      return false;
    }
    boolean loginOk = login == null || constantTimeEquals(login, decoded.substring(0, colon));
    return loginOk && constantTimeEquals(password, decoded.substring(colon + 1));
  }

  public static boolean constantTimeEquals(String expected, String actual) {
    return actual != null && MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
        actual.getBytes(StandardCharsets.UTF_8));
  }

  public static String md5Hex(String text) {
    try {
      byte[] digest = MessageDigest.getInstance("MD5").digest(text.getBytes(StandardCharsets.UTF_8));
      StringBuilder hex = new StringBuilder();
      for (byte b : digest) {
        hex.append(String.format("%02x", b));
      }
      return hex.toString();
    } catch (java.security.NoSuchAlgorithmException ex) {
      throw new IllegalStateException(ex);
    }
  }
}
