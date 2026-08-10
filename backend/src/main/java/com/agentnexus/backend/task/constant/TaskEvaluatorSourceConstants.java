package com.agentnexus.backend.task.constant;

import java.util.List;

/**
 * 评测任务绑定评估器来源常量。
 *
 * 创建日期：2026-08-10。
 */
public final class TaskEvaluatorSourceConstants {
  private TaskEvaluatorSourceConstants() {
  }

  public static final String PRESET = "preset";
  public static final String CUSTOM = "custom";
  public static final List<String> SUPPORTED_SOURCES = List.of(PRESET, CUSTOM);
}
