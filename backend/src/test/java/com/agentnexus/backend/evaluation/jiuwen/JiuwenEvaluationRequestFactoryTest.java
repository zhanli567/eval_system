package com.agentnexus.backend.evaluation.jiuwen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.agentnexus.backend.evaluation.EvaluationRequest;
import com.agentnexus.backend.evaluation.EvaluatorSnapshot;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JiuwenEvaluationRequestFactoryTest {

  @Test
  void createsStructuredRequestFromDatasetRowAndAgentResult() {
    Map<String, String> rowValues = Map.of("question", "1+1", "answer", "2");
    Map<String, String> outputs = Map.of("answer", "2", "text", "2");
    List<Map<String, Object>> trajectory =
        List.of(Map.of("sequence", 1, "eventType", "agent_finish"));
    Map<String, Object> metrics = new LinkedHashMap<>();
    metrics.put("latencyMs", 42L);
    metrics.put("inputTokens", null);
    EvaluatorSnapshot evaluator = new EvaluatorSnapshot(
        "v1",
        "exact_match",
        "",
        "",
        "",
        "",
        BigDecimal.ZERO,
        BigDecimal.ONE,
        BigDecimal.ONE,
        Map.of());

    EvaluationRequest request = new JiuwenEvaluationRequestFactory().create(
        "case-1",
        rowValues,
        outputs,
        trajectory,
        metrics,
        evaluator);

    assertEquals("case-1", request.caseId());
    assertEquals(rowValues, request.inputs());
    assertEquals(rowValues, request.labels());
    assertEquals(outputs, request.prediction());
    assertEquals(trajectory, request.trajectory());
    assertEquals(metrics, request.metrics());
    assertEquals(evaluator, request.evaluator());
  }

  @Test
  void usesPreparedExpectedAndActualForExactMatchAnswer() {
    EvaluatorSnapshot evaluator = new EvaluatorSnapshot(
        "v1",
        "exact_match",
        "",
        "",
        "",
        "",
        BigDecimal.ZERO,
        BigDecimal.ONE,
        BigDecimal.ONE,
        Map.of("preparedParams", Map.of("expected", "gold", "actual", "candidate")));

    EvaluationRequest request = new JiuwenEvaluationRequestFactory().create(
        "case-1",
        Map.of("reference", "gold"),
        Map.of("text", "candidate"),
        List.of(),
        Map.of(),
        evaluator);

    assertEquals("gold", request.labels().get("answer"));
    assertEquals("candidate", request.prediction().get("answer"));
  }
}
