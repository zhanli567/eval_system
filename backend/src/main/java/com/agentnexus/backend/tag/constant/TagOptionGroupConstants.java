package com.agentnexus.backend.tag.constant;

import java.util.List;

/**
 * 标签选项分组常量。
 *
 * 创建日期：2026-08-10。
 */
public final class TagOptionGroupConstants {
  private TagOptionGroupConstants() {
  }

  public static final String PASS = "pass";
  public static final String FAIL = "fail";
  public static final String TRUE_OPTION_NAME = "True";
  public static final String FALSE_OPTION_NAME = "False";
  public static final List<String> SUPPORTED_GROUPS = List.of(PASS, FAIL);
}
