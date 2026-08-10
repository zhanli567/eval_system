package com.agentnexus.backend.dataset.constant;

import java.util.List;

/**
 * 评测集字段类型常量。
 *
 * 创建日期：2026-08-10。
 */
public final class DatasetFieldTypeConstants {
  private DatasetFieldTypeConstants() {
  }

  public static final String STRING = "string";
  public static final String NUMBER = "number";
  public static final String BOOLEAN = "boolean";
  public static final List<String> SUPPORTED_TYPES = List.of(STRING, NUMBER, BOOLEAN);
}
