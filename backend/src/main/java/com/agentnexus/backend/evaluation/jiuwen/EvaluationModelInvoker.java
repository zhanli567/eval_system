package com.agentnexus.backend.evaluation.jiuwen;

@FunctionalInterface
public interface EvaluationModelInvoker {
  String invoke(String modelId, String modelName, String prompt);
}
