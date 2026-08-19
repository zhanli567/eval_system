package com.agentnexus.backend.evaluation.jiuwen;

import com.agentnexus.backend.evaluation.EvaluatorSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openjiuwen.agent_evolving.evaluator.BaseEvaluator;
import com.openjiuwen.agent_evolving.evaluator.MetricEvaluator;

public final class JiuwenEvaluatorFactory {
  private final EvaluationModelInvoker modelInvoker;
  private final ObjectMapper objectMapper;

  public JiuwenEvaluatorFactory() {
    this((modelId, modelName, prompt) -> {
      throw new UnsupportedOperationException("Jiuwen LLM evaluator model invoker is not configured");
    });
  }

  public JiuwenEvaluatorFactory(EvaluationModelInvoker modelInvoker) {
    this(modelInvoker, new ObjectMapper().findAndRegisterModules());
  }

  JiuwenEvaluatorFactory(EvaluationModelInvoker modelInvoker, ObjectMapper objectMapper) {
    this.modelInvoker = modelInvoker;
    this.objectMapper = objectMapper;
  }

  public BaseEvaluator create(EvaluatorSnapshot snapshot) {
    if ("exact_match".equals(snapshot.evaluatorType())) {
      Object configured = snapshot.options().get("normalize");
      boolean normalize = configured == null || Boolean.parseBoolean(String.valueOf(configured));
      return new MetricEvaluator(new ExactAnswerMetric(normalize));
    }
    if ("llm".equals(snapshot.evaluatorType())) {
      return new ScoreLlmEvaluator(snapshot, modelInvoker, objectMapper);
    }
    throw new UnsupportedOperationException(
        "Jiuwen evaluator type is not connected: " + snapshot.evaluatorType());
  }
}
