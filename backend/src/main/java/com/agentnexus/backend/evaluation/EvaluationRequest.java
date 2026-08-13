package com.agentnexus.backend.evaluation;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 一次评估器执行所需的完整、不可变输入。 */
public record EvaluationRequest(
    String caseId,
    Map<String, Object> inputs,
    Map<String, Object> labels,
    Map<String, Object> prediction,
    List<Map<String, Object>> trajectory,
    Map<String, Object> metrics,
    EvaluatorSnapshot evaluator
) {
  public EvaluationRequest {
    caseId = requireText(caseId, "caseId");
    inputs = requireNonEmptyMap(inputs, "inputs");
    labels = requireNonEmptyMap(labels, "labels");
    prediction = prediction == null ? Map.of() : Map.copyOf(prediction);
    trajectory = trajectory == null ? List.of() : trajectory.stream().map(Map::copyOf).toList();
    metrics = metrics == null ? Map.of() : Map.copyOf(metrics);
    evaluator = Objects.requireNonNull(evaluator, "evaluator");
  }

  private static Map<String, Object> requireNonEmptyMap(Map<String, Object> value, String name) {
    if (value == null || value.isEmpty()) {
      throw new IllegalArgumentException(name + " must not be empty");
    }
    return Map.copyOf(value);
  }

  private static String requireText(String value, String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }
}
