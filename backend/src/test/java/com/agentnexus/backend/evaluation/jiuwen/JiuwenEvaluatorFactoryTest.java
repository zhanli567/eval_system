package com.agentnexus.backend.evaluation.jiuwen;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.agentnexus.backend.evaluation.EvaluatorSnapshot;
import com.openjiuwen.agent_evolving.evaluator.MetricEvaluator;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JiuwenEvaluatorFactoryTest {

  @Test
  void createsMetricEvaluatorForExactMatch() {
    EvaluatorSnapshot snapshot = snapshot("exact_match", Map.of("normalize", true));

    assertInstanceOf(MetricEvaluator.class, new JiuwenEvaluatorFactory().create(snapshot));
  }

  @Test
  void rejectsEvaluatorTypesThatAreNotConnectedYet() {
    EvaluatorSnapshot snapshot = snapshot("llm", Map.of());

    assertThrows(UnsupportedOperationException.class, () -> new JiuwenEvaluatorFactory().create(snapshot));
  }

  private EvaluatorSnapshot snapshot(String type, Map<String, Object> options) {
    return new EvaluatorSnapshot(
        "v1", type, "", "", "", "", BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ONE, options);
  }
}
