package com.agentnexus.backend.evaluator.constant;

import java.util.List;

/**
 * 评估器类型常量。
 *
 * 创建日期：2026-08-10。
 */
public final class EvaluatorTypeConstants {
  private EvaluatorTypeConstants() {
  }

  public static final String LLM = "llm";
  public static final String CODE = "code";
  public static final List<String> SUPPORTED_TYPES = List.of(LLM, CODE);
}
