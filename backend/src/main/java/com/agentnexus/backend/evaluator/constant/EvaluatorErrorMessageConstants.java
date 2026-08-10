package com.agentnexus.backend.evaluator.constant;

import java.util.List;

/**
 * 评估器异常提示常量。
 *
 * 创建日期：2026-08-10。
 */
public final class EvaluatorErrorMessageConstants {
  private static final String NAME_SEPARATOR = "、";
  private static final String USED_BY_TASK_SUFFIX = "”使用，不能删除";
  private static final String EVALUATOR_USED_BY_TASK_PREFIX = "评估器已被评测任务“";
  private static final String EVALUATOR_VERSION_USED_BY_TASK_PREFIX = "评估器版本已被评测任务“";
  private static final String PRESET_CATEGORY_ID_DUPLICATED_PREFIX = "预置评估器分类ID重复：";
  private static final String PRESET_EVALUATOR_ID_DUPLICATED_PREFIX = "预置评估器ID重复：";
  private static final String PRESET_CATEGORY_NOT_FOUND_PREFIX = "预置评估器分类不存在：";
  private static final String PRESET_TYPE_UNSUPPORTED_PREFIX = "预置评估器类型仅支持llm/code：";
  private static final String PRESET_SCORE_RANGE_INVALID_PREFIX = "预置评估器评分范围不合法：";
  private static final String PRESET_THRESHOLD_INVALID_PREFIX = "预置评估器通过阈值不在评分范围内：";
  private static final String PRESET_LLM_PROMPT_PARAM_REQUIRED_PREFIX = "LLM预置评估器Prompt至少需要一个${参数名}：";
  private static final String PRESET_LLM_PARAM_NOT_REFERENCED_PREFIX = "LLM预置评估器参数未在Prompt中引用：";
  private static final String PRESET_CODE_PARAM_REQUIRED_PREFIX = "Code预置评估器至少需要一个参数：";
  private static final String PRESET_PARAM_NAME_INVALID_PREFIX = "预置评估器参数名不合法：";
  private static final String PRESET_PARAM_NAME_DUPLICATED_PREFIX = "预置评估器参数名重复：";
  private static final String PRESET_PARAM_TYPE_UNSUPPORTED_PREFIX = "预置评估器参数类型仅支持string/number/boolean：";
  private static final String PRESET_LLM_PROMPT_REQUIRED_PREFIX = "LLM预置评估器Prompt不能为空：";
  private static final String PRESET_CODE_EXECUTE_REQUIRED_PREFIX = "Code预置评估器执行函数不能为空：";

  public static final String TRIAL_REQUEST_REQUIRED = "评估器试运行参数不能为空";
  public static final String DUPLICATE_EVALUATOR_NAME = "当前空间已存在同名评估器";
  public static final String EVALUATOR_VERSION_NOT_FOUND = "评估器版本不存在";
  public static final String ONLY_DRAFT_VERSION_CAN_MODIFY = "只有草稿版本允许修改";
  public static final String DRAFT_VERSION_NOT_FOUND = "草稿版本不存在";
  public static final String CODE_EVALUATOR_UNSUPPORTED = "暂不支持Code型评估器";
  public static final String EVALUATOR_NOT_FOUND = "评估器不存在";
  public static final String ONLY_CREATOR_CAN_DELETE_EVALUATOR = "仅创建人可以删除评估器";
  public static final String ONLY_CREATOR_CAN_DELETE_EVALUATOR_VERSION = "仅创建人可以删除评估器版本";
  public static final String DRAFT_VERSION_CANNOT_DELETE = "草稿版本不能删除";
  public static final String EVALUATOR_REQUEST_REQUIRED = "评估器参数不能为空";
  public static final String EVALUATOR_TYPE_CANNOT_MODIFY = "评估器类型创建后不允许修改";
  public static final String EVALUATOR_NAME_REQUIRED = "评估器名称不能为空";
  public static final String EVALUATOR_NAME_TOO_LONG = "评估器名称不能超过50个字符";
  public static final String DESCRIPTION_TOO_LONG = "描述不能超过200个字符";
  public static final String EVALUATOR_TYPE_REQUIRED = "评估器类型不能为空";
  public static final String UNSUPPORTED_EVALUATOR_TYPE = "评估器类型仅支持llm/code";
  public static final String MODEL_REQUIRED = "请选择模型";
  public static final String PROMPT_REQUIRED = "Prompt不能为空";
  public static final String PROMPT_TOO_LONG = "Prompt不能超过2000个字符";
  public static final String EXECUTE_CODE_REQUIRED = "执行函数不能为空";
  public static final String EXECUTE_CODE_TOO_LONG = "执行函数不能超过10000个字符";
  public static final String PARAM_REQUIRED = "请至少配置一个变量";
  public static final String PARAM_NAME_TOO_LONG = "变量名不能超过64个字符";
  public static final String PARAM_NAME_DUPLICATED = "变量名不能重复";
  public static final String PROMPT_PARAM_REQUIRED = "Prompt至少需要包含一个${参数名}参数";
  public static final String UNSUPPORTED_PARAM_TYPE = "变量类型仅支持string/number/boolean";
  public static final String PARAM_DESCRIPTION_TOO_LONG = "变量描述不能超过200个字符";
  public static final String SCORE_RANGE_INVALID = "评分范围最大值必须大于最小值";
  public static final String PASS_THRESHOLD_INVALID = "通过阈值必须位于评分范围内";
  public static final String CODE_EVALUATOR_TRIAL_UNSUPPORTED = "暂不支持Code型评估器试运行";
  public static final String PRESET_EVALUATOR_NOT_FOUND = "预置评估器不存在";
  public static final String PRESET_PARAM_NAME_REQUIRED = "预置评估器参数名不能为空";
  public static final String PRESET_CATEGORY_ID_REQUIRED = "预置评估器分类ID不能为空";
  public static final String PRESET_CATEGORY_NAME_REQUIRED = "预置评估器分类名称不能为空";
  public static final String PRESET_EVALUATOR_ID_REQUIRED = "预置评估器ID不能为空";
  public static final String PRESET_EVALUATOR_NAME_REQUIRED = "预置评估器名称不能为空";

  private EvaluatorErrorMessageConstants() {
  }

  /**
   * 返回评估器被评测任务占用的提示。
   *
   * @param taskNames 任务名称列表
   * @return 返回评估器被评测任务占用的提示
   */
  public static String evaluatorUsedByTasks(List<String> taskNames) {
    return EVALUATOR_USED_BY_TASK_PREFIX + String.join(NAME_SEPARATOR, taskNames) + USED_BY_TASK_SUFFIX;
  }

  /**
   * 返回评估器版本被评测任务占用的提示。
   *
   * @param taskNames 任务名称列表
   * @return 返回评估器版本被评测任务占用的提示
   */
  public static String evaluatorVersionUsedByTasks(List<String> taskNames) {
    return EVALUATOR_VERSION_USED_BY_TASK_PREFIX + String.join(NAME_SEPARATOR, taskNames) + USED_BY_TASK_SUFFIX;
  }

  /**
   * 返回预置评估器分类ID重复提示。
   *
   * @param categoryId 分类id
   * @return 返回预置评估器分类ID重复提示
   */
  public static String duplicatePresetCategoryId(String categoryId) {
    return PRESET_CATEGORY_ID_DUPLICATED_PREFIX + categoryId;
  }

  /**
   * 返回预置评估器ID重复提示。
   *
   * @param evaluatorId 评估器id
   * @return 返回预置评估器ID重复提示
   */
  public static String duplicatePresetEvaluatorId(String evaluatorId) {
    return PRESET_EVALUATOR_ID_DUPLICATED_PREFIX + evaluatorId;
  }

  /**
   * 返回预置评估器分类不存在提示。
   *
   * @param categoryId 分类id
   * @return 返回预置评估器分类不存在提示
   */
  public static String presetCategoryNotFound(String categoryId) {
    return PRESET_CATEGORY_NOT_FOUND_PREFIX + categoryId;
  }

  /**
   * 返回预置评估器类型不支持提示。
   *
   * @param evaluatorId 评估器id
   * @return 返回预置评估器类型不支持提示
   */
  public static String unsupportedPresetType(String evaluatorId) {
    return PRESET_TYPE_UNSUPPORTED_PREFIX + evaluatorId;
  }

  /**
   * 返回预置评估器评分范围不合法提示。
   *
   * @param evaluatorId 评估器id
   * @return 返回预置评估器评分范围不合法提示
   */
  public static String invalidPresetScoreRange(String evaluatorId) {
    return PRESET_SCORE_RANGE_INVALID_PREFIX + evaluatorId;
  }

  /**
   * 返回预置评估器通过阈值不合法提示。
   *
   * @param evaluatorId 评估器id
   * @return 返回预置评估器通过阈值不合法提示
   */
  public static String invalidPresetThreshold(String evaluatorId) {
    return PRESET_THRESHOLD_INVALID_PREFIX + evaluatorId;
  }

  /**
   * 返回LLM预置评估器Prompt参数缺失提示。
   *
   * @param evaluatorId 评估器id
   * @return 返回LLM预置评估器Prompt参数缺失提示
   */
  public static String presetLlmPromptParamRequired(String evaluatorId) {
    return PRESET_LLM_PROMPT_PARAM_REQUIRED_PREFIX + evaluatorId;
  }

  /**
   * 返回LLM预置评估器参数未引用提示。
   *
   * @param paramName 参数名称
   * @return 返回LLM预置评估器参数未引用提示
   */
  public static String presetLlmParamNotReferenced(String paramName) {
    return PRESET_LLM_PARAM_NOT_REFERENCED_PREFIX + paramName;
  }

  /**
   * 返回Code预置评估器参数缺失提示。
   *
   * @param evaluatorId 评估器id
   * @return 返回Code预置评估器参数缺失提示
   */
  public static String presetCodeParamRequired(String evaluatorId) {
    return PRESET_CODE_PARAM_REQUIRED_PREFIX + evaluatorId;
  }

  /**
   * 返回预置评估器参数名不合法提示。
   *
   * @param paramName 参数名称
   * @return 返回预置评估器参数名不合法提示
   */
  public static String invalidPresetParamName(String paramName) {
    return PRESET_PARAM_NAME_INVALID_PREFIX + paramName;
  }

  /**
   * 返回预置评估器参数名重复提示。
   *
   * @param paramName 参数名称
   * @return 返回预置评估器参数名重复提示
   */
  public static String duplicatePresetParamName(String paramName) {
    return PRESET_PARAM_NAME_DUPLICATED_PREFIX + paramName;
  }

  /**
   * 返回预置评估器参数类型不支持提示。
   *
   * @param dataType 参数类型
   * @return 返回预置评估器参数类型不支持提示
   */
  public static String unsupportedPresetParamType(String dataType) {
    return PRESET_PARAM_TYPE_UNSUPPORTED_PREFIX + dataType;
  }

  /**
   * 返回LLM预置评估器Prompt不能为空提示。
   *
   * @param evaluatorId 评估器id
   * @return 返回LLM预置评估器Prompt不能为空提示
   */
  public static String presetLlmPromptRequired(String evaluatorId) {
    return PRESET_LLM_PROMPT_REQUIRED_PREFIX + evaluatorId;
  }

  /**
   * 返回Code预置评估器执行函数不能为空提示。
   *
   * @param evaluatorId 评估器id
   * @return 返回Code预置评估器执行函数不能为空提示
   */
  public static String presetCodeExecuteRequired(String evaluatorId) {
    return PRESET_CODE_EXECUTE_REQUIRED_PREFIX + evaluatorId;
  }
}
