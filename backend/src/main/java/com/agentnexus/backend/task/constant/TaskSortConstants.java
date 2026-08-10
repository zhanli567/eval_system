package com.agentnexus.backend.task.constant;

/**
 * 评测任务排序字段常量。
 *
 * 创建日期：2026-08-10。
 */
public final class TaskSortConstants {
  private TaskSortConstants() {
  }

  public static final String CREATED_DATE = "createdDate";
  public static final String ASC = "asc";
  public static final String SQL_ASC = "ASC";
  public static final String SQL_DESC = "DESC";
  public static final String TASK_CREATED_DATE_COLUMN = "t.created_date";
  public static final String TASK_LAST_UPDATED_DATE_COLUMN = "t.last_updated_date";
}
