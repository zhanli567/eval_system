package com.agentnexus.backend.evaluation;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;

/** 创建任务时冻结的评估器版本配置。 */
public record EvaluatorSnapshot(
    String versionId,
    String evaluatorType,
    String modelId,
    String modelName,
    String prompt,
    String executeCode,
    BigDecimal scoreMin,
    BigDecimal scoreMax,
    BigDecimal passThreshold,
    Map<String, Object> options
) {
  public EvaluatorSnapshot {
    versionId = Objects.requireNonNullElse(versionId, "");
    evaluatorType = requireText(evaluatorType, "evaluatorType");
    modelId = Objects.requireNonNullElse(modelId, "");
    modelName = Objects.requireNonNullElse(modelName, "");
    prompt = Objects.requireNonNullElse(prompt, "");
    executeCode = Objects.requireNonNullElse(executeCode, "");
    scoreMin = Objects.requireNonNull(scoreMin, "scoreMin");
    scoreMax = Objects.requireNonNull(scoreMax, "scoreMax");
    passThreshold = Objects.requireNonNull(passThreshold, "passThreshold");
    if (scoreMin.compareTo(scoreMax) >= 0) {
      throw new IllegalArgumentException("scoreMin must be less than scoreMax");
    }
    if (passThreshold.compareTo(scoreMin) < 0 || passThreshold.compareTo(scoreMax) > 0) {
      throw new IllegalArgumentException("passThreshold must be within score range");
    }
    options = options == null ? Map.of() : Map.copyOf(options);
  }

  private static String requireText(String value, String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }
}
