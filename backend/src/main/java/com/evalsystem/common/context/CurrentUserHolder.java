package com.evalsystem.common.context;

import java.util.concurrent.atomic.AtomicInteger;

public final class CurrentUserHolder {
  public static final ThreadLocal<CurrentUser> CURRENT_USER = new ThreadLocal<>();
  public static final AtomicInteger ACTIVE_BINGINGS = new AtomicInteger(0);

  private CurrentUserHolder() {
  }

  public static void set(CurrentUser currentUser) {
    if (CURRENT_USER.get() == null && currentUser != null) {
      ACTIVE_BINGINGS.incrementAndGet();
    }
    CURRENT_USER.set(currentUser);
  }

  public static CurrentUser get() {
    return CURRENT_USER.get();
  }

  public static void clear() {
    if (CURRENT_USER.get() != null) {
      ACTIVE_BINGINGS.decrementAndGet();
    }
    CURRENT_USER.remove();
  }

  public static int activeBindings() {
    return ACTIVE_BINGINGS.get();
  }
}
