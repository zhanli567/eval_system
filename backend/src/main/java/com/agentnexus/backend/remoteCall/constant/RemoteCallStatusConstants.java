package com.agentnexus.backend.remoteCall.constant;

import com.agentnexus.backend.task.constant.TaskStatusConstants;

/**
 * 远程调用状态常量。
 *
 * 创建日期：2026-08-10。
 */
public final class RemoteCallStatusConstants {
  private RemoteCallStatusConstants() {
  }

  public static final String COMPLETED = TaskStatusConstants.COMPLETED;
  public static final String FAILED = TaskStatusConstants.FAILED;
  public static final String ACTIVE = "ACTIVE";
  public static final String SUCCESS_CODE = "200";
}
