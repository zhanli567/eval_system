package com.evalsystem.common.context;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class CurrentSpaceHolderTest {

  @AfterEach
  void tearDown() {
    CurrentSpaceHolder.clear();
  }

  @Test
  void bindsCurrentSpaceToCurrentThread() {
    CurrentSpaceHolder.set("space-1");

    assertThat(CurrentSpaceHolder.get()).isEqualTo("space-1");
    assertThat(CurrentSpaceHolder.activeBingings()).isEqualTo(1);
  }

  @Test
  void clearRemovesCurrentSpaceAndDecrementsActiveBindings() {
    CurrentSpaceHolder.set("space-1");

    CurrentSpaceHolder.clear();

    assertThat(CurrentSpaceHolder.get()).isNull();
    assertThat(CurrentSpaceHolder.activeBingings()).isZero();
  }

  @Test
  void runWithSpaceRestoresPreviousSpace() {
    CurrentSpaceHolder.set("outer");

    CurrentSpaceHolder.runWithSpace("inner", () -> assertThat(CurrentSpaceHolder.get()).isEqualTo("inner"));

    assertThat(CurrentSpaceHolder.get()).isEqualTo("outer");
    assertThat(CurrentSpaceHolder.activeBingings()).isEqualTo(1);
  }

  @Test
  void callWithSpaceReturnsActionResultAndClearsWhenNoPreviousSpace() {
    String result = CurrentSpaceHolder.callWithSpace("space-1", CurrentSpaceHolder::get);

    assertThat(result).isEqualTo("space-1");
    assertThat(CurrentSpaceHolder.get()).isNull();
    assertThat(CurrentSpaceHolder.activeBingings()).isZero();
  }
}
