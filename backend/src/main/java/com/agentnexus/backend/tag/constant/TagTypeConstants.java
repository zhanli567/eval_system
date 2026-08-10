package com.agentnexus.backend.tag.constant;

import java.util.List;

/**
 * 标签类型常量。
 *
 * 创建日期：2026-08-10。
 */
public final class TagTypeConstants {
  private TagTypeConstants() {
  }

  public static final String CATEGORY = "category";
  public static final String BOOLEAN = "boolean";
  public static final String NUMBER = "number";
  public static final String TEXT = "text";
  public static final List<String> SUPPORTED_TYPES = List.of(CATEGORY, BOOLEAN, NUMBER, TEXT);
}
