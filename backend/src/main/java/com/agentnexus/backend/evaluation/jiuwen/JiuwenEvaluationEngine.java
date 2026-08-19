package com.agentnexus.backend.evaluation.jiuwen;

import com.agentnexus.backend.evaluation.EvaluationEngine;
import com.agentnexus.backend.evaluation.EvaluationOutcome;
import com.agentnexus.backend.evaluation.EvaluationRequest;
import com.openjiuwen.agent_evolving.dataset.EvaluatedCase;
import com.openjiuwen.agent_evolving.evaluator.BaseEvaluator;

public final class JiuwenEvaluationEngine implements EvaluationEngine {
  private final JiuwenCaseAdapter caseAdapter;
  private final JiuwenEvaluatorFactory evaluatorFactory;
  private final JiuwenResultMapper resultMapper;

  public JiuwenEvaluationEngine() {
    this(new JiuwenCaseAdapter(), new JiuwenEvaluatorFactory(), new JiuwenResultMapper());
  }

  public JiuwenEvaluationEngine(EvaluationModelInvoker modelInvoker) {
    this(new JiuwenCaseAdapter(), new JiuwenEvaluatorFactory(modelInvoker), new JiuwenResultMapper());
  }

  JiuwenEvaluationEngine(
      JiuwenCaseAdapter caseAdapter,
      JiuwenEvaluatorFactory evaluatorFactory,
      JiuwenResultMapper resultMapper
  ) {
    this.caseAdapter = caseAdapter;
    this.evaluatorFactory = evaluatorFactory;
    this.resultMapper = resultMapper;
  }

  @Override
  public EvaluationOutcome evaluate(EvaluationRequest request) {
    JiuwenCaseInput input = caseAdapter.adapt(request);
    BaseEvaluator evaluator = evaluatorFactory.create(request.evaluator());
    EvaluatedCase evaluated = evaluator.evaluate(input.caseValue(), input.predict());
    return resultMapper.map(evaluated, request.evaluator());
  }
}
