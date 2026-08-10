package com.agentnexus.backend.task.constant;

import java.util.List;

/**
 * 评测任务评估器参数来源常量。
 *
 * 创建日期：2026-08-10。
 */
public final class TaskParamSourceConstants {
  private TaskParamSourceConstants() {
  }

  public static final String DATASET_FIELD = "dataset_field";
  public static final String APP_OUTPUT = "app_output";
  public static final List<String> SUPPORTED_SOURCES = List.of(DATASET_FIELD, APP_OUTPUT);
}
