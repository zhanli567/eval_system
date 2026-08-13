package com.agentnexus.backend.evaluation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EvaluationRequestTest {

  @Test
  void requestDefensivelyCopiesEvaluationSnapshot() {
    EvaluationRequest request = new EvaluationRequest(
        "case-1",
        Map.of("question", "1+1"),
        Map.of("expected", "2"),
        Map.of("answer", "2"),
        List.of(Map.of("eventType", "agent_finish")),
        Map.of("latencyMs", 10),
        new EvaluatorSnapshot(
            "evaluator-version-1", "exact_match", "", "", "", "",
            BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ONE, Map.of("normalize", true)));

    assertEquals("case-1", request.caseId());
    assertEquals("2", request.prediction().get("answer"));
    assertThrows(UnsupportedOperationException.class, () -> request.inputs().put("x", "y"));
    assertThrows(UnsupportedOperationException.class, () -> request.trajectory().add(Map.of()));
  }

  @Test
  void requestRequiresCaseInputsLabelsAndEvaluatorSnapshot() {
    EvaluatorSnapshot snapshot = new EvaluatorSnapshot(
        "v1", "exact_match", "", "", "", "",
        BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ONE, Map.of());

    assertThrows(IllegalArgumentException.class, () -> new EvaluationRequest(
        "case-1", Map.of(), Map.of("expected", "2"), Map.of(), List.of(), Map.of(), snapshot));
    assertThrows(IllegalArgumentException.class, () -> new EvaluationRequest(
        "case-1", Map.of("question", "q"), Map.of(), Map.of(), List.of(), Map.of(), snapshot));
  }
}
