package com.agentnexus.backend.common.context;

public final class TaskCookieHolder {
  private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

  private TaskCookieHolder() {
  }

  public static void set(String cookie) {
    CURRENT.set(cookie);
  }

  public static String get() {
    return CURRENT.get();
  }

  public static void clear() {
    CURRENT.remove();
  }
}
