package com.agentnexus.backend.task.api.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * 评测任务数据行的评估器执行结果。
 *
 * @param id 评估器结果ID
 * @param taskItemId 任务数据行ID
 * @param taskEvaluatorId 任务评估器绑定ID
 * @param evaluatorName 评估器名称
 * @param evaluatorType 评估器类型
 * @param versionName 版本名称
 * @param status 执行状态
 * @param score 得分
 * @param passResult 通过结果
 * @param resultValue 结果说明
 * @param errorMessage 错误信息
 * @param startedAt 开始时间
 * @param finishedAt 结束时间
 * @param params 参数展示列表
 */
public record TaskEvaluatorResultDto(
    String id,
    String taskItemId,
    String taskEvaluatorId,
    String evaluatorName,
    String evaluatorType,
    String versionName,
    String status,
    BigDecimal score,
    String passResult,
    String resultValue,
    String errorMessage,
    String startedAt,
    String finishedAt,
    List<TaskEvaluatorParamDisplay> params
) {
  /**
   * 构造不包含参数展示信息的评估器执行结果。
   *
   * @param id 评估器结果ID
   * @param taskItemId 任务数据行ID
   * @param taskEvaluatorId 任务评估器绑定ID
   * @param evaluatorName 评估器名称
   * @param evaluatorType 评估器类型
   * @param versionName 版本名称
   * @param status 执行状态
   * @param score 得分
   * @param passResult 通过结果
   * @param resultValue 结果说明
   * @param errorMessage 错误信息
   * @param startedAt 开始时间
   * @param finishedAt 结束时间
   */
  public TaskEvaluatorResultDto(
      String id,
      String taskItemId,
      String taskEvaluatorId,
      String evaluatorName,
      String evaluatorType,
      String versionName,
      String status,
      BigDecimal score,
      String passResult,
      String resultValue,
      String errorMessage,
      String startedAt,
      String finishedAt
  ) {
    this(
        id,
        taskItemId,
        taskEvaluatorId,
        evaluatorName,
        evaluatorType,
        versionName,
        status,
        score,
        passResult,
        resultValue,
        errorMessage,
        startedAt,
        finishedAt,
        List.of());
  }

  /**
   * 返回替换展示名称和类型后的评估器执行结果。
   *
   * @param evaluatorName 评估器名称
   * @param evaluatorType 评估器类型
   * @param versionName 版本名称
   * @return 替换展示信息后的评估器执行结果
   */
  public TaskEvaluatorResultDto withDisplay(String evaluatorName, String evaluatorType, String versionName) {
    return new TaskEvaluatorResultDto(
        id,
        taskItemId,
        taskEvaluatorId,
        evaluatorName,
        evaluatorType,
        versionName,
        status,
        score,
        passResult,
        resultValue,
        errorMessage,
        startedAt,
        finishedAt,
        params);
  }

  /**
   * 返回附加参数展示信息后的评估器执行结果。
   *
   * @param params 参数展示列表
   * @return 附加参数展示信息后的评估器执行结果
   */
  public TaskEvaluatorResultDto withParams(List<TaskEvaluatorParamDisplay> params) {
    return new TaskEvaluatorResultDto(
        id,
        taskItemId,
        taskEvaluatorId,
        evaluatorName,
        evaluatorType,
        versionName,
        status,
        score,
        passResult,
        resultValue,
        errorMessage,
        startedAt,
        finishedAt,
        params == null ? List.of() : params);
  }
}
