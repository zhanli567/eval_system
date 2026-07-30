package com.agentnexus.backend.task.api.dto.response;

import java.util.List;

/**
 * 评测任务绑定的评估器维度信息。
 *
 * @param taskEvaluatorId 任务评估器绑定ID
 * @param evaluatorSource 评估器来源
 * @param evaluatorId 评估器ID
 * @param evaluatorVersionId 评估器版本ID
 * @param evaluatorName 评估器名称
 * @param evaluatorType 评估器类型
 * @param versionName 版本名称
 * @param status 执行状态
 * @param passCount 通过数量
 * @param completedCount 已完成数量
 * @param totalCount 总数量
 * @param passRate 通过率
 * @param displayOrder 展示顺序
 * @param params 参数展示列表
 */
public record TaskEvaluatorDimension(
    String taskEvaluatorId,
    String evaluatorSource,
    String evaluatorId,
    String evaluatorVersionId,
    String evaluatorName,
    String evaluatorType,
    String versionName,
    String status,
    Integer passCount,
    Integer completedCount,
    Integer totalCount,
    Double passRate,
    Integer displayOrder,
    List<TaskEvaluatorParamDisplay> params
) {
  /**
   * 构造不包含参数展示信息的评估器维度信息。
   *
   * @param taskEvaluatorId 任务评估器绑定ID
   * @param evaluatorSource 评估器来源
   * @param evaluatorId 评估器ID
   * @param evaluatorVersionId 评估器版本ID
   * @param evaluatorName 评估器名称
   * @param evaluatorType 评估器类型
   * @param versionName 版本名称
   * @param status 执行状态
   * @param passCount 通过数量
   * @param completedCount 已完成数量
   * @param totalCount 总数量
   * @param passRate 通过率
   * @param displayOrder 展示顺序
   */
  public TaskEvaluatorDimension(
      String taskEvaluatorId,
      String evaluatorSource,
      String evaluatorId,
      String evaluatorVersionId,
      String evaluatorName,
      String evaluatorType,
      String versionName,
      String status,
      Integer passCount,
      Integer completedCount,
      Integer totalCount,
      Double passRate,
      Integer displayOrder
  ) {
    this(
        taskEvaluatorId,
        evaluatorSource,
        evaluatorId,
        evaluatorVersionId,
        evaluatorName,
        evaluatorType,
        versionName,
        status,
        passCount,
        completedCount,
        totalCount,
        passRate,
        displayOrder,
        List.of());
  }

  /**
   * 返回替换展示名称和类型后的评估器维度信息。
   *
   * @param evaluatorName 评估器名称
   * @param evaluatorType 评估器类型
   * @param versionName 版本名称
   * @return 替换展示信息后的评估器维度信息
   */
  public TaskEvaluatorDimension withDisplay(String evaluatorName, String evaluatorType, String versionName) {
    return new TaskEvaluatorDimension(
        taskEvaluatorId,
        evaluatorSource,
        evaluatorId,
        evaluatorVersionId,
        evaluatorName,
        evaluatorType,
        versionName,
        status,
        passCount,
        completedCount,
        totalCount,
        passRate,
        displayOrder,
        params);
  }

  /**
   * 返回附加参数展示信息后的评估器维度信息。
   *
   * @param params 参数展示列表
   * @return 附加参数展示信息后的评估器维度信息
   */
  public TaskEvaluatorDimension withParams(List<TaskEvaluatorParamDisplay> params) {
    return new TaskEvaluatorDimension(
        taskEvaluatorId,
        evaluatorSource,
        evaluatorId,
        evaluatorVersionId,
        evaluatorName,
        evaluatorType,
        versionName,
        status,
        passCount,
        completedCount,
        totalCount,
        passRate,
        displayOrder,
        params == null ? List.of() : params);
  }
}
