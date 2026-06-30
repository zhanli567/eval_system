package com.agentnexus.backend.common.context;

import com.agentnexus.backend.common.security.CurrentUser;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.function.Supplier;

public final class RepositoryContext {
  private RepositoryContext() {
  }

  public static String spaceId() {
    return Objects.toString(CurrentSpaceHolder.get(), "");
  }

  public static String userId() {
    CurrentUser user = CurrentUserHolder.get();
    return user == null ? "" : Objects.toString(user.userId(), "");
  }

  public static String displayName() {
    CurrentUser user = CurrentUserHolder.get();
    return user == null ? "" : Objects.toString(user.displayName(), "");
  }

  public static <T> T callWithCurrentSpace(Supplier<T> action) {
    return CurrentSpaceHolder.callWithSpace(spaceId(), action);
  }

  public static void fillCreated(Object entity) {
    set(entity, "setSpaceId", spaceId());
    set(entity, "setCreatedBy", userId());
    set(entity, "setCreatedByName", displayName());
    fillUpdated(entity);
  }

  public static void fillUpdated(Object entity) {
    set(entity, "setLastUpdatedBy", userId());
    set(entity, "setLastUpdatedByName", displayName());
  }

  private static void set(Object entity, String methodName, String value) {
    try {
      entity.getClass().getMethod(methodName, String.class).invoke(entity, value);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
      throw new IllegalStateException(entity.getClass().getName() + " missing " + methodName, ex);
    }
  }
}
