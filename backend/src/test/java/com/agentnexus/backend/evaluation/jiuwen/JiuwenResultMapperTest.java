package com.agentnexus.backend.evaluation.jiuwen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.agentnexus.backend.evaluation.EvaluationOutcome;
import com.agentnexus.backend.evaluation.EvaluatorSnapshot;
import com.openjiuwen.agent_evolving.dataset.Case;
import com.openjiuwen.agent_evolving.dataset.EvaluatedCase;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JiuwenResultMapperTest {

  @Test
  void mapsNormalizedJiuwenScoreBackToConfiguredRawRange() {
    Case caseValue = new Case(Map.of("question", "q"), Map.of("expected", "a"), null, "case-1");
    EvaluatedCase evaluated = new EvaluatedCase(
        caseValue, Map.of("answer", "a"), 0.75d, "基本正确", Map.of("exact_match", 0.75d));
    EvaluatorSnapshot snapshot = new EvaluatorSnapshot(
        "v1", "exact_match", "", "", "", "",
        BigDecimal.ONE, new BigDecimal("5"), new BigDecimal("4"), Map.of());

    EvaluationOutcome outcome = new JiuwenResultMapper().map(evaluated, snapshot);

    assertEquals(new BigDecimal("4.00"), outcome.rawScore());
    assertEquals(0.75d, outcome.normalizedScore(), 0.000001d);
    assertEquals("pass", outcome.passResult());
    assertEquals("基本正确", outcome.reason());
    assertEquals(Map.of("exact_match", 0.75d), outcome.perMetric());
  }
}
