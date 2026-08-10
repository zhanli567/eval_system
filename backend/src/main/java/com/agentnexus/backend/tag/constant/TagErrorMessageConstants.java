package com.agentnexus.backend.tag.constant;

import java.util.List;

/**
 * 标签异常提示常量。
 *
 * 创建日期：2026-08-10。
 */
public final class TagErrorMessageConstants {
  private static final String NAME_SEPARATOR = "、";
  private static final String TAG_USED_BY_TASK_PREFIX = "标签已被评测任务“";
  private static final String USED_BY_TASK_SUFFIX = "”使用，不能删除";

  public static final String DUPLICATE_TAG_NAME_IN_SPACE = "当前空间已存在同名标签";
  public static final String TAG_TYPE_CANNOT_MODIFY = "标签类型创建后不能修改";
  public static final String DUPLICATE_TAG_NAME = "标签名称不能重复";
  public static final String ONLY_CREATOR_CAN_DELETE_TAG = "仅创建人可以删除标签";
  public static final String TAG_ID_REQUIRED = "标签ID不能为空";
  public static final String TAG_NOT_FOUND = "标签不存在";
  public static final String TAG_REQUEST_REQUIRED = "标签参数不能为空";
  public static final String TAG_NAME_REQUIRED = "标签名称不能为空";
  public static final String TAG_NAME_TOO_LONG = "标签名称不能超过50个字符";
  public static final String TAG_DESCRIPTION_TOO_LONG = "标签描述不能超过200个字符";
  public static final String TAG_TYPE_REQUIRED = "标签类型不能为空";
  public static final String UNSUPPORTED_TAG_TYPE = "标签类型仅支持category、boolean、number、text";
  public static final String CATEGORY_OPTIONS_REQUIRED = "分类标签请至少配置一个Pass选项和一个Fail选项";
  public static final String TAG_OPTION_TOO_LONG = "标签选项不能超过50个字符";
  public static final String TAG_OPTION_DUPLICATED = "标签选项不能重复";
  public static final String CATEGORY_OPTION_LIMIT_EXCEEDED = "Pass和Fail选项每组最多支持5个";
  public static final String OPTION_GROUP_REQUIRED = "选项分组不能为空";
  public static final String UNSUPPORTED_OPTION_GROUP = "选项分组仅支持pass、fail";
  public static final String NUMBER_TAG_SCORE_REQUIRED = "数字标签请维护评分范围和通过阈值";
  public static final String NUMBER_TAG_SCORE_MUST_BE_POSITIVE = "数字标签评分范围和通过阈值必须为正整数";
  public static final String NUMBER_TAG_SCORE_RANGE_INVALID = "数字标签评分范围最大值必须大于最小值";
  public static final String NUMBER_TAG_THRESHOLD_INVALID = "通过阈值必须介于评分范围最小值和最大值之间";

  private TagErrorMessageConstants() {
  }

  /**
   * 返回标签被评测任务占用的提示。
   *
   * @param taskNames 任务名称列表
   * @return 返回标签被评测任务占用的提示
   */
  public static String tagUsedByTasks(List<String> taskNames) {
    return TAG_USED_BY_TASK_PREFIX + String.join(NAME_SEPARATOR, taskNames) + USED_BY_TASK_SUFFIX;
  }
}
