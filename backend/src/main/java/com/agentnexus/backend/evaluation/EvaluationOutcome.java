package com.agentnexus.backend.evaluation;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Map;
import java.util.Objects;

/** Eval-System 使用的评测结果，原始业务分数与 Jiuwen 归一化分数分开保存。 */
public record EvaluationOutcome(
    String status,
    BigDecimal rawScore,
    double normalizedScore,
    String passResult,
    String reason,
    Map<String, Double> perMetric,
    Map<String, Object> rawResult,
    String errorMessage
) {
  public EvaluationOutcome {
    status = Objects.requireNonNullElse(status, "failed");
    passResult = Objects.requireNonNullElse(passResult, "");
    reason = Objects.requireNonNullElse(reason, "");
    perMetric = perMetric == null ? Map.of() : Map.copyOf(perMetric);
    rawResult = rawResult == null ? Map.of() : Map.copyOf(rawResult);
    errorMessage = Objects.requireNonNullElse(errorMessage, "");
  }

  public static EvaluationOutcome completed(
      BigDecimal rawScore,
      BigDecimal scoreMin,
      BigDecimal scoreMax,
      BigDecimal passThreshold,
      String reason,
      Map<String, Double> perMetric,
      Map<String, Object> rawResult
  ) {
    Objects.requireNonNull(rawScore, "rawScore");
    validateRange(scoreMin, scoreMax, passThreshold);
    double normalized = rawScore.subtract(scoreMin)
        .divide(scoreMax.subtract(scoreMin), MathContext.DECIMAL64)
        .doubleValue();
    String pass = rawScore.compareTo(passThreshold) >= 0 ? "pass" : "fail";
    return new EvaluationOutcome(
        "completed", rawScore, normalized, pass, reason, perMetric, rawResult, "");
  }

  public static EvaluationOutcome failed(String errorMessage) {
    return new EvaluationOutcome("failed", null, 0.0d, "", "", Map.of(), Map.of(), errorMessage);
  }

  private static void validateRange(BigDecimal min, BigDecimal max, BigDecimal threshold) {
    Objects.requireNonNull(min, "scoreMin");
    Objects.requireNonNull(max, "scoreMax");
    Objects.requireNonNull(threshold, "passThreshold");
    if (min.compareTo(max) >= 0) {
      throw new IllegalArgumentException("scoreMin must be less than scoreMax");
    }
    if (threshold.compareTo(min) < 0 || threshold.compareTo(max) > 0) {
      throw new IllegalArgumentException("passThreshold must be within score range");
    }
  }
}
