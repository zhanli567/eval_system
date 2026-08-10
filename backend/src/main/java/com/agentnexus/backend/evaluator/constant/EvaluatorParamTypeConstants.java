package com.agentnexus.backend.evaluator.constant;

import com.agentnexus.backend.dataset.constant.DatasetFieldTypeConstants;
import java.util.List;

/**
 * 评估器参数类型常量。
 *
 * 创建日期：2026-08-10。
 */
public final class EvaluatorParamTypeConstants {
  private EvaluatorParamTypeConstants() {
  }

  public static final String STRING = DatasetFieldTypeConstants.STRING;
  public static final String NUMBER = DatasetFieldTypeConstants.NUMBER;
  public static final String BOOLEAN = DatasetFieldTypeConstants.BOOLEAN;
  public static final List<String> SUPPORTED_TYPES = List.of(STRING, NUMBER, BOOLEAN);
}
