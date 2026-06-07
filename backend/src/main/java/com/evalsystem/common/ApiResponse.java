package com.evalsystem.common;

public record ApiResponse<T>(
    Integer code,
    String msg,
    T data
) {
  public static <T> ApiResponse<T> ok(T data) {
    return new ApiResponse<>(0, "success", data);
  }

  public static <T> ApiResponse<T> fail(Integer code, String msg) {
    return new ApiResponse<>(code, msg, null);
  }
}
