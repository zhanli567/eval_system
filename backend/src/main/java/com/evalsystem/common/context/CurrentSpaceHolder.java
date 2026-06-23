package com.evalsystem.common.context;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public final class CurrentSpaceHolder {
  public static final ThreadLocal<String> CURRENT_SPACE_ID = new ThreadLocal<>();
  public static final AtomicInteger ACTIVE_BINDINGS = new AtomicInteger(0);

  private CurrentSpaceHolder() {
  }

  public static void set(String spaceId) {
    if (CURRENT_SPACE_ID.get() == null && spaceId != null) {
      ACTIVE_BINDINGS.incrementAndGet();
    }
    CURRENT_SPACE_ID.set(spaceId);
  }

  public static String get() {
    return CURRENT_SPACE_ID.get();
  }

  public static void clear() {
    if (CURRENT_SPACE_ID.get() != null) {
      ACTIVE_BINDINGS.decrementAndGet();
    }
    CURRENT_SPACE_ID.remove();
  }

  public static void runWithSpace(String spaceId, Runnable action) {
    callWithSpace(spaceId, () -> {
      action.run();
      return null;
    });
  }

  public static <T> T callWithSpace(String spaceId, Supplier<T> action) {
    String previous = get();
    boolean needsSwitch = !Objects.equals(previous, spaceId);
    if (needsSwitch) {
      set(spaceId);
    }
    try {
      return action.get();
    } finally {
      if (needsSwitch) {
        if (previous == null) {
          clear();
        } else {
          set(previous);
        }
      }
    }
  }

  public static int activeBingings() {
    return ACTIVE_BINDINGS.get();
  }
}
