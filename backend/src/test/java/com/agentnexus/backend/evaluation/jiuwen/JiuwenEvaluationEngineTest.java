package com.agentnexus.backend.evaluation.jiuwen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.agentnexus.backend.evaluation.EvaluationOutcome;
import com.agentnexus.backend.evaluation.EvaluationRequest;
import com.agentnexus.backend.evaluation.EvaluatorSnapshot;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
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

  @Test
  void evaluatesLlmWithInjectedModelInvoker() {
    AtomicReference<String> capturedModelId = new AtomicReference<>();
    AtomicReference<String> capturedModelName = new AtomicReference<>();
    AtomicReference<String> capturedPrompt = new AtomicReference<>();
    JiuwenEvaluationEngine engine = new JiuwenEvaluationEngine((modelId, modelName, prompt) -> {
      capturedModelId.set(modelId);
      capturedModelName.set(modelName);
      capturedPrompt.set(prompt);
      return """
          ```json
          {"score": 4, "reason": "回答基本正确"}
          ```
          """;
    });
    EvaluationRequest request = new EvaluationRequest(
        "case-llm",
        Map.of("question", "解释蓝天为什么是蓝色"),
        Map.of("answer", "瑞利散射"),
        Map.of("answer", "主要因为瑞利散射"),
        List.of(Map.of("sequence", 1, "eventType", "agent_finish")),
        Map.of("latencyMs", 100L),
        new EvaluatorSnapshot(
            "v2", "llm", "model-1", "judge-model", "ignored raw prompt", "",
            BigDecimal.ONE, new BigDecimal("5"), new BigDecimal("3"),
            Map.of("renderedPrompt", "请判断回答质量")));

    EvaluationOutcome outcome = engine.evaluate(request);

    assertEquals("model-1", capturedModelId.get());
    assertEquals("judge-model", capturedModelName.get());
    assertEquals("请判断回答质量", capturedPrompt.get());
    assertEquals("completed", outcome.status());
    assertEquals(new BigDecimal("4.00"), outcome.rawScore());
    assertEquals("pass", outcome.passResult());
    assertEquals("回答基本正确", outcome.reason());
    assertEquals(0.75d, outcome.perMetric().get("llm"), 0.000001d);
  }
}
