package com.agentnexus.backend.task.constant;

/**
 * 评测任务异常提示常量。
 *
 * 创建日期：2026-08-10。
 */
public final class TaskErrorMessageConstants {
  private static final String REQUIRED_EVALUATOR_MAPPING_PREFIX = "请完成评估器必填字段映射：";
  private static final String TEXT_TAG_INPUT_PREFIX = "请输入文本标签：";
  private static final String NUMBER_TAG_INPUT_PREFIX = "请输入数字标签：";
  private static final String NUMBER_TAG_MIN_PREFIX = "数字标签不能小于最小值：";
  private static final String NUMBER_TAG_MAX_PREFIX = "数字标签不能大于最大值：";
  private static final String TAG_OPTION_NOT_FOUND_PREFIX = "标签选项不存在：";
  private static final String TAG_OPTION_SELECT_PREFIX = "请选择标签选项：";

  public static final String DUPLICATE_TASK_NAME = "当前空间已存在同名评测任务";
  public static final String COMPLETED_TASK_CANNOT_RESTART = "评测完成的任务不能重新开始";
  public static final String ONLY_RUNNING_TASK_CAN_STOP = "仅进行中的评测任务可以停止";
  public static final String ONLY_CREATOR_CAN_DELETE_TASK = "仅创建人可以删除评测任务";
  public static final String TASK_DELETE_STATUS_UNSUPPORTED = "仅待执行、评测完成、评测失败和已中止的任务可删除";
  public static final String TAG_SELECT_REQUIRED = "请选择标签";
  public static final String TAG_NOT_FOUND = "标签不存在";
  public static final String TASK_TAG_DUPLICATED = "该任务已添加此标签";
  public static final String TASK_TAG_LIMIT_EXCEEDED = "标签最多添加5个";
  public static final String TASK_TAG_NOT_FOUND = "任务标签不存在";
  public static final String STOPPED_TASK_CANNOT_ANNOTATE = "已中止的评测任务不能继续标注";
  public static final String ANNOTATION_RESULT_REQUIRED = "请提交标注结果";
  public static final String TAG_NOT_BELONG_TO_TASK = "标签不属于当前任务";
  public static final String TASK_REQUEST_REQUIRED = "评测任务参数不能为空";
  public static final String TASK_NAME_REQUIRED = "任务名称不能为空";
  public static final String TASK_NAME_TOO_LONG = "任务名称不能超过50个字符";
  public static final String DESCRIPTION_TOO_LONG = "描述不能超过200个字符";
  public static final String DATASET_SELECT_REQUIRED = "请选择评测集";
  public static final String DATASET_VERSION_SELECT_REQUIRED = "请选择评测集版本";
  public static final String DATASET_VERSION_NOT_FOUND = "评测集版本不存在";
  public static final String DATASET_VERSION_NOT_PUBLISHED = "评测任务请选择已发布的评测集版本";
  public static final String DATASET_NOT_FOUND = "评测集不存在";
  public static final String DATASET_VERSION_ITEM_REQUIRED = "评测集版本中暂无数据，不能创建任务";
  public static final String EVALUATOR_OR_TAG_REQUIRED = "请至少添加一个评估器或标签";
  public static final String AGENT_APP_SELECT_REQUIRED = "请选择智能体应用";
  public static final String AGENT_APP_VERSION_SELECT_REQUIRED = "请选择智能体应用版本";
  public static final String APP_FIELD_MAPPING_DUPLICATED = "应用入参不能重复映射";
  public static final String APP_FIELD_TYPE_UNSUPPORTED = "应用入参类型仅支持string/number/boolean";
  public static final String APP_FIELD_MAPPING_FIELD_SELECT_REQUIRED = "请选择应用入参映射的评测集字段";
  public static final String APP_FIELD_MAPPING_DATASET_FIELD_NOT_FOUND = "应用入参映射的评测集字段不存在";
  public static final String EVALUATOR_LIMIT_EXCEEDED = "评估器最多添加5个";
  public static final String EVALUATOR_SELECT_REQUIRED = "请选择评估器";
  public static final String CODE_EVALUATOR_UNSUPPORTED = "暂不支持Code型评估器";
  public static final String PRESET_EVALUATOR_MODEL_SELECT_REQUIRED = "请选择预置评估器模型";
  public static final String CUSTOM_EVALUATOR_VERSION_SELECT_REQUIRED = "请选择自定义评估器版本";
  public static final String CUSTOM_EVALUATOR_VERSION_NOT_PUBLISHED = "评测任务请选择已发布的自定义评估器版本";
  public static final String CUSTOM_EVALUATOR_VERSION_NOT_MATCHED = "自定义评估器版本不属于所选评估器";
  public static final String EVALUATOR_DUPLICATED = "评估器不能重复添加";
  public static final String EVALUATOR_REQUIRED = "请至少添加一个评估器";
  public static final String EVALUATOR_DATASET_FIELD_NOT_FOUND = "评估器字段映射的评测集字段不存在";
  public static final String APP_OUTPUT_MAPPING_UNSUPPORTED = "未关联应用时不能映射到应用输出";
  public static final String TAG_DUPLICATED = "标签不能重复添加";
  public static final String TASK_ID_REQUIRED = "评测任务ID不能为空";
  public static final String TASK_NOT_FOUND = "评测任务不存在";
  public static final String TASK_ITEM_ID_REQUIRED = "任务数据行ID不能为空";
  public static final String TASK_ITEM_NOT_FOUND = "任务数据行不存在";
  public static final String TASK_STATUS_UNSUPPORTED = "评测状态不支持";
  public static final String APP_TYPE_UNSUPPORTED = "应用类型仅支持不关联应用或智能体";
  public static final String EVALUATOR_SOURCE_SELECT_REQUIRED = "请选择评估器类型";
  public static final String EVALUATOR_SOURCE_UNSUPPORTED = "评估器类型仅支持预置或自定义";
  public static final String PARAM_SOURCE_SELECT_REQUIRED = "请选择字段映射来源";
  public static final String PARAM_SOURCE_UNSUPPORTED = "字段映射来源不支持";
  public static final String DATASET_FIELD_SELECT_REQUIRED = "请选择评测集字段";

  private TaskErrorMessageConstants() {
  }

  /**
   * 返回评估器必填字段未映射提示。
   *
   * @param paramName 参数名称
   * @return 返回评估器必填字段未映射提示
   */
  public static String requiredEvaluatorMapping(String paramName) {
    return REQUIRED_EVALUATOR_MAPPING_PREFIX + paramName;
  }

  /**
   * 返回文本标签输入提示。
   *
   * @param tagName 标签名称
   * @return 返回文本标签输入提示
   */
  public static String textTagInputRequired(String tagName) {
    return TEXT_TAG_INPUT_PREFIX + tagName;
  }

  /**
   * 返回数字标签输入提示。
   *
   * @param tagName 标签名称
   * @return 返回数字标签输入提示
   */
  public static String numberTagInputRequired(String tagName) {
    return NUMBER_TAG_INPUT_PREFIX + tagName;
  }

  /**
   * 返回数字标签小于最小值提示。
   *
   * @param tagName 标签名称
   * @return 返回数字标签小于最小值提示
   */
  public static String numberTagLessThanMin(String tagName) {
    return NUMBER_TAG_MIN_PREFIX + tagName;
  }

  /**
   * 返回数字标签大于最大值提示。
   *
   * @param tagName 标签名称
   * @return 返回数字标签大于最大值提示
   */
  public static String numberTagGreaterThanMax(String tagName) {
    return NUMBER_TAG_MAX_PREFIX + tagName;
  }

  /**
   * 返回标签选项不存在提示。
   *
   * @param tagName 标签名称
   * @return 返回标签选项不存在提示
   */
  public static String tagOptionNotFound(String tagName) {
    return TAG_OPTION_NOT_FOUND_PREFIX + tagName;
  }

  /**
   * 返回请选择标签选项提示。
   *
   * @param tagName 标签名称
   * @return 返回请选择标签选项提示
   */
  public static String tagOptionSelectRequired(String tagName) {
    return TAG_OPTION_SELECT_PREFIX + tagName;
  }
}
