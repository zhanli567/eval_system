package com.agentnexus.backend.evaluation.jiuwen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.agentnexus.backend.evaluation.EvaluationRequest;
import com.agentnexus.backend.evaluation.EvaluatorSnapshot;
import com.openjiuwen.agent_evolving.dataset.Case;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JiuwenCaseAdapterTest {

  @Test
  void convertsEvalRequestToJiuwenCaseAndStructuredPrediction() {
    EvaluationRequest request = new EvaluationRequest(
        "case-7",
        Map.of("question", "天气如何"),
        Map.of("expected", "晴"),
        Map.of("answer", "晴", "text", "晴"),
        List.of(Map.of("sequence", 1, "eventType", "agent_finish")),
        Map.of("latencyMs", 12),
        new EvaluatorSnapshot("v1", "exact_match", "", "", "", "",
            BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ONE, Map.of()));

    JiuwenCaseInput adapted = new JiuwenCaseAdapter().adapt(request);
    Case caseValue = adapted.caseValue();

    assertEquals("case-7", caseValue.getCaseId());
    assertEquals(Map.of("question", "天气如何"), caseValue.getInputs());
    assertEquals(Map.of("expected", "晴"), caseValue.getLabel());
    assertEquals("晴", adapted.predict().get("answer"));
    assertEquals(request.trajectory(), adapted.predict().get("trajectory"));
    assertEquals(request.metrics(), adapted.predict().get("metrics"));
  }
}
