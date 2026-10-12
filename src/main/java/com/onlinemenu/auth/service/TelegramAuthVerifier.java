package com.onlinemenu.auth.service;

import com.onlinemenu.common.constant.ExceptionMessageCodes;
import com.onlinemenu.common.exception.BusinessException;
import com.onlinemenu.common.exception.UnauthorizedException;
import com.onlinemenu.common.util.HashUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Checks the data the Telegram Login Widget hands the browser, as described in
 * https://core.telegram.org/widgets/login#checking-authorization: an HMAC-SHA256 of the sorted
 * fields, keyed with the SHA-256 of the bot token.
 */
@Component
public class TelegramAuthVerifier {

  private static final String FIELD_HASH = "hash";
  private static final String FIELD_AUTH_DATE = "auth_date";

  @Value("${app.telegram.bot-username:}")
  private String botUsername;

  @Value("${app.telegram.bot-token:}")
  private String botToken;

  @Value("${app.telegram.auth-max-age-seconds:86400}")
  private long authMaxAgeSeconds;

  public record TelegramUser(long id, String firstName, String lastName, String username) {

  }

  public boolean isEnabled() {
    return StringUtils.hasText(botUsername) && StringUtils.hasText(botToken);
  }

  public String getBotUsername() {
    return isEnabled() ? botUsername : null;
  }

  public TelegramUser verify(Map<String, Object> data) {
    if (!isEnabled()) {
      throw new BusinessException(ExceptionMessageCodes.TELEGRAM_DISABLED,
          HttpStatus.SERVICE_UNAVAILABLE);
    }
    Object hash = data.get(FIELD_HASH);
    if (!(hash instanceof String receivedHash) || !hashMatches(data, receivedHash)) {
      throw new UnauthorizedException(ExceptionMessageCodes.TELEGRAM_INVALID);
    }
    long authDate = parseLong(data.get(FIELD_AUTH_DATE));
    if (Instant.now().getEpochSecond() - authDate > authMaxAgeSeconds) {
      throw new UnauthorizedException(ExceptionMessageCodes.TELEGRAM_INVALID);
    }
    return new TelegramUser(parseLong(data.get("id")), string(data.get("first_name")),
        string(data.get("last_name")), string(data.get("username")));
  }

  private boolean hashMatches(Map<String, Object> data, String receivedHash) {
    String dataCheckString = data.entrySet().stream()
        .filter(entry -> !FIELD_HASH.equals(entry.getKey()) && entry.getValue() != null)
        .sorted(Map.Entry.comparingByKey())
        .map(entry -> entry.getKey() + "=" + entry.getValue())
        .collect(Collectors.joining("\n"));
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      byte[] secretKey = HashUtils.sha256Bytes(botToken.getBytes(StandardCharsets.UTF_8));
      mac.init(new SecretKeySpec(secretKey, "HmacSHA256"));
      byte[] expected = mac.doFinal(dataCheckString.getBytes(StandardCharsets.UTF_8));
      byte[] received = HexFormat.of().parseHex(receivedHash);
      return MessageDigest.isEqual(expected, received);
    } catch (IllegalArgumentException ex) {
      return false;
    } catch (GeneralSecurityException ex) {
      throw new IllegalStateException("HmacSHA256 not available", ex);
    }
  }

  private static long parseLong(Object value) {
    try {
      return Long.parseLong(String.valueOf(value));
    } catch (NumberFormatException ex) {
      throw new UnauthorizedException(ExceptionMessageCodes.TELEGRAM_INVALID);
    }
  }

  private static String string(Object value) {
    return value == null ? null : value.toString().trim();
  }
}
