package com.agentnexus.backend.task.constant;

import java.util.List;

/**
 * 评测任务执行状态常量。
 *
 * 创建日期：2026-08-10。
 */
public final class TaskStatusConstants {
  private TaskStatusConstants() {
  }

  public static final String PENDING = "pending";
  public static final String RUNNING = "running";
  public static final String COMPLETED = "completed";
  public static final String FAILED = "failed";
  public static final String STOPPED = "stopped";
  public static final String ANNOTATION_PENDING = "annotation_pending";
  public static final String SKIPPED = "skipped";
  public static final List<String> SUPPORTED_TASK_STATUSES = List.of(PENDING, RUNNING, COMPLETED, FAILED, STOPPED);
  public static final List<String> RESTARTABLE_STATUSES = List.of(RUNNING, FAILED, STOPPED);
  public static final List<String> STOPPABLE_STATUSES = List.of(PENDING, RUNNING);
  public static final List<String> DELETABLE_STATUSES = List.of(PENDING, COMPLETED, FAILED, STOPPED);
  public static final List<String> FINISHED_EVALUATOR_RESULT_STATUSES = List.of(COMPLETED, SKIPPED);
}
