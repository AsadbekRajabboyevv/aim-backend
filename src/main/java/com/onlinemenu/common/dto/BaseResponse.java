package com.onlinemenu.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BaseResponse<T>(
    boolean success,
    String message,
    T data,
    List<String> errors,
    Instant timestamp
) {

  public static <T> BaseResponse<T> success(T data) {
    return new BaseResponse<>(true, null, data, null, Instant.now());
  }

  public static <T> BaseResponse<T> success(String message, T data) {
    return new BaseResponse<>(true, message, data, null, Instant.now());
  }

  public static BaseResponse<Void> error(String message) {
    return new BaseResponse<>(false, message, null, null, Instant.now());
  }

  public static BaseResponse<Void> error(String message, List<String> errors) {
    return new BaseResponse<>(false, message, null, errors, Instant.now());
  }

  public static <T> BaseResponse<T> failure(String message, T data) {
    return new BaseResponse<>(false, message, data, null, Instant.now());
  }
}
