package com.agentnexus.backend.dataset.constant;

import java.util.List;

/**
 * 评测集异常提示常量。
 *
 * 创建日期：2026-08-10。
 */
public final class DatasetErrorMessageConstants {
  private static final String NAME_SEPARATOR = "、";
  private static final String USED_BY_TASK_SUFFIX = "”使用，不能删除";
  private static final String DATASET_USED_BY_TASK_PREFIX = "评测集已被评测任务“";
  private static final String DATASET_VERSION_USED_BY_TASK_PREFIX = "评测集版本已被评测任务“";
  private static final String DUPLICATE_DATASET_FIELD_NAME_PREFIX = "评测集表头存在重复列名：";
  private static final String DUPLICATE_EXCEL_HEADER_PREFIX = "Excel表头存在重复列：";
  private static final String MISSING_REQUIRED_FIELD_PREFIX = "Excel缺少必填列：";
  private static final String VALUE_REQUIRED_SUFFIX = "不能为空";
  private static final String VALUE_MUST_BE_NUMBER_SUFFIX = "应为数字";
  private static final String VALUE_MUST_BE_BOOLEAN_SUFFIX = "应为布尔值true或false";

  public static final String DATASET_REQUEST_REQUIRED = "评测集参数不能为空";
  public static final String DUPLICATE_DATASET_NAME = "当前空间已存在同名评测集";
  public static final String DATASET_NOT_FOUND = "评测集不存在";
  public static final String ONLY_CREATOR_CAN_DELETE_DATASET = "仅创建人可以删除评测集";
  public static final String PLEASE_MAINTAIN_HEADER_FIRST = "请先维护表头";
  public static final String DATASET_VERSION_NOT_FOUND = "评测集版本不存在";
  public static final String ONLY_CREATOR_CAN_DELETE_DATASET_VERSION = "仅创建人可以删除评测集版本";
  public static final String DRAFT_VERSION_CANNOT_DELETE = "草稿版本不能删除";
  public static final String CANNOT_OVERWRITE_DRAFT_WITH_DRAFT = "不能用草稿覆盖草稿";
  public static final String PLEASE_MAINTAIN_HEADER = "请维护表头";
  public static final String DATASET_FIELD_LIMIT_EXCEEDED = "评测集最多支持10列";
  public static final String FIELD_NAME_REQUIRED = "列名不能为空";
  public static final String FIELD_NAME_DUPLICATED = "列名不能重复";
  public static final String UNSUPPORTED_FIELD_TYPE = "字段类型仅支持string、number、boolean";
  public static final String DATASET_NAME_REQUIRED = "评测集名称不能为空";
  public static final String DATASET_NAME_TOO_LONG = "评测集名称不能超过50个字符";
  public static final String DESCRIPTION_TOO_LONG = "描述不能超过200个字符";
  public static final String EXCEL_FILE_REQUIRED = "请上传Excel文件";
  public static final String UNSUPPORTED_EXCEL_FILE = "仅支持xlsx或xls文件";
  public static final String EXCEL_SHEET_REQUIRED = "Excel文件没有工作表";
  public static final String EXCEL_HEADER_ROW_REQUIRED = "Excel第一行必须是表头";
  public static final String EXCEL_READ_FAILED = "读取Excel文件失败";
  public static final String EXCEL_HEADER_NO_MATCHED_FIELD = "Excel表头未匹配到评测集字段";
  public static final String ONLY_DRAFT_VERSION_CAN_MODIFY = "只有草稿版本允许修改";

  private DatasetErrorMessageConstants() {
  }

  /**
   * 返回评测集被评测任务占用的提示。
   *
   * @param taskNames 任务名称列表
   * @return 返回评测集被评测任务占用的提示
   */
  public static String datasetUsedByTasks(List<String> taskNames) {
    return DATASET_USED_BY_TASK_PREFIX + String.join(NAME_SEPARATOR, taskNames) + USED_BY_TASK_SUFFIX;
  }

  /**
   * 返回评测集版本被评测任务占用的提示。
   *
   * @param taskNames 任务名称列表
   * @return 返回评测集版本被评测任务占用的提示
   */
  public static String datasetVersionUsedByTasks(List<String> taskNames) {
    return DATASET_VERSION_USED_BY_TASK_PREFIX + String.join(NAME_SEPARATOR, taskNames) + USED_BY_TASK_SUFFIX;
  }

  /**
   * 返回评测集表头重复列名提示。
   *
   * @param fieldName 字段名称
   * @return 返回评测集表头重复列名提示
   */
  public static String duplicateDatasetFieldName(String fieldName) {
    return DUPLICATE_DATASET_FIELD_NAME_PREFIX + fieldName;
  }

  /**
   * 返回Excel表头重复列提示。
   *
   * @param header 表头名称
   * @return 返回Excel表头重复列提示
   */
  public static String duplicateExcelHeader(String header) {
    return DUPLICATE_EXCEL_HEADER_PREFIX + header;
  }

  /**
   * 返回Excel缺少必填列提示。
   *
   * @param fieldNames 字段名称列表
   * @return 返回Excel缺少必填列提示
   */
  public static String missingRequiredFields(List<String> fieldNames) {
    return MISSING_REQUIRED_FIELD_PREFIX + String.join(NAME_SEPARATOR, fieldNames);
  }

  /**
   * 返回字段值不能为空提示。
   *
   * @param position 字段位置
   * @return 返回字段值不能为空提示
   */
  public static String fieldValueRequired(String position) {
    return position + VALUE_REQUIRED_SUFFIX;
  }

  /**
   * 返回字段值应为数字提示。
   *
   * @param position 字段位置
   * @return 返回字段值应为数字提示
   */
  public static String fieldValueMustBeNumber(String position) {
    return position + VALUE_MUST_BE_NUMBER_SUFFIX;
  }

  /**
   * 返回字段值应为布尔值提示。
   *
   * @param position 字段位置
   * @return 返回字段值应为布尔值提示
   */
  public static String fieldValueMustBeBoolean(String position) {
    return position + VALUE_MUST_BE_BOOLEAN_SUFFIX;
  }
}
