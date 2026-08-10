package com.agentnexus.backend.task.constant;

import java.util.List;

/**
 * 评测任务应用类型常量。
 *
 * 创建日期：2026-08-10。
 */
public final class TaskAppTypeConstants {
  private TaskAppTypeConstants() {
  }

  public static final String NONE = "none";
  public static final String AGENT = "agent";
  public static final List<String> SUPPORTED_TYPES = List.of(NONE, AGENT);
}
