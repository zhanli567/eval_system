package com.agentnexus.backend.evaluation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EvaluationOutcomeTest {

  @Test
  void completedOutcomeKeepsRawScoreAndCalculatesNormalizedScoreAndPassResult() {
    EvaluationOutcome outcome = EvaluationOutcome.completed(
        new BigDecimal("4.5"),
        new BigDecimal("1"),
        new BigDecimal("5"),
        new BigDecimal("4"),
        "回答完整",
        Map.of("quality", 0.875d),
        Map.of("source", "jiuwen"));

    assertEquals(new BigDecimal("4.5"), outcome.rawScore());
    assertEquals(0.875d, outcome.normalizedScore(), 0.000001d);
    assertEquals("pass", outcome.passResult());
    assertEquals("回答完整", outcome.reason());
    assertEquals(Map.of("quality", 0.875d), outcome.perMetric());
  }

  @Test
  void completedOutcomeUsesRawThresholdAndDoesNotClampOutOfRangeScore() {
    EvaluationOutcome outcome = EvaluationOutcome.completed(
        new BigDecimal("6"),
        BigDecimal.ONE,
        new BigDecimal("5"),
        new BigDecimal("4"),
        "越界分数仍保留",
        Map.of(),
        Map.of());

    assertEquals(new BigDecimal("6"), outcome.rawScore());
    assertEquals(1.25d, outcome.normalizedScore(), 0.000001d);
    assertEquals("pass", outcome.passResult());
  }

  @Test
  void completedOutcomeRejectsInvalidScoreRange() {
    assertThrows(IllegalArgumentException.class, () -> EvaluationOutcome.completed(
        BigDecimal.ONE,
        BigDecimal.ONE,
        BigDecimal.ONE,
        BigDecimal.ONE,
        "",
        Map.of(),
        Map.of()));
  }
}
