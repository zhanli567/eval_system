package com.agentnexus.backend.evaluation;

/** 可替换的评测执行引擎。 */
@FunctionalInterface
public interface EvaluationEngine {
  EvaluationOutcome evaluate(EvaluationRequest request);
}
