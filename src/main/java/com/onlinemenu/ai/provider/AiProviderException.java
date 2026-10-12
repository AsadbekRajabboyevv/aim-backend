package com.onlinemenu.ai.provider;

public class AiProviderException extends RuntimeException {

  public enum Kind {
    RATE_LIMITED,
    MODEL_NOT_FOUND,
    UNAVAILABLE,
    OTHER
  }

  private final Kind kind;

  public AiProviderException(String message) {
    this(message, Kind.OTHER);
  }

  public AiProviderException(String message, Kind kind) {
    super(message);
    this.kind = kind;
  }

  public AiProviderException(String message, Throwable cause) {
    super(message, cause);
    this.kind = Kind.OTHER;
  }

  public Kind getKind() {
    return kind;
  }

  public static Kind kindOf(int status) {
    return switch (status) {
      case 429 -> Kind.RATE_LIMITED;
      case 404 -> Kind.MODEL_NOT_FOUND;
      case 500, 502, 503, 504 -> Kind.UNAVAILABLE;
      default -> Kind.OTHER;
    };
  }
}
