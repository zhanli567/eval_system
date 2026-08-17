package com.agentnexus.backend.evaluation.jiuwen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.agentnexus.backend.evaluation.EvaluationOutcome;
import com.agentnexus.backend.evaluation.EvaluationRequest;
import com.agentnexus.backend.evaluation.EvaluatorSnapshot;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JiuwenEvaluationEngineTest {

  @Test
  void evaluatesExactMatchThroughJiuwenEngine() {
    Map<String, Object> metrics = new LinkedHashMap<>();
    metrics.put("latencyMs", 10L);
    metrics.put("inputTokens", null);
    EvaluationRequest request = new EvaluationRequest(
        "case-1",
        Map.of("question", "1+1"),
        Map.of("answer", "2"),
        Map.of("answer", "2"),
        List.of(Map.of("sequence", 1, "eventType", "agent_finish")),
        metrics,
        new EvaluatorSnapshot(
            "v1", "exact_match", "", "", "", "",
            BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ONE, Map.of("normalize", true)));

    EvaluationOutcome outcome = new JiuwenEvaluationEngine().evaluate(request);

    assertEquals("completed", outcome.status());
    assertEquals(new BigDecimal("1.00"), outcome.rawScore());
    assertEquals("pass", outcome.passResult());
    assertEquals(1.0d, outcome.perMetric().get("exact_match"));
  }
}
