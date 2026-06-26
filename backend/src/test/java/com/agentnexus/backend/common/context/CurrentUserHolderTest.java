package com.agentnexus.backend.common.context;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class CurrentUserHolderTest {

  @AfterEach
  void tearDown() {
    CurrentUserHolder.clear();
  }

  @Test
  void bindsCurrentUserToCurrentThread() {
    CurrentUser user = new CurrentUser("user-1", "User One", Set.of("space-1"));

    CurrentUserHolder.set(user);

    assertThat(CurrentUserHolder.get()).isSameAs(user);
    assertThat(CurrentUserHolder.activeBindings()).isEqualTo(1);
  }

  @Test
  void doesNotDoubleCountWhenThreadAlreadyHasUser() {
    CurrentUserHolder.set(new CurrentUser("user-1", "User One", Set.of("space-1")));

    CurrentUserHolder.set(new CurrentUser("user-2", "User Two", Set.of("space-2")));

    assertThat(CurrentUserHolder.get().userId()).isEqualTo("user-2");
    assertThat(CurrentUserHolder.activeBindings()).isEqualTo(1);
  }

  @Test
  void clearRemovesCurrentUserAndDecrementsActiveBindings() {
    CurrentUserHolder.set(new CurrentUser("user-1", "User One", Set.of("space-1")));

    CurrentUserHolder.clear();

    assertThat(CurrentUserHolder.get()).isNull();
    assertThat(CurrentUserHolder.activeBindings()).isZero();
  }
}
