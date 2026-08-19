package com.agentnexus.backend.evaluation.jiuwen;

import com.agentnexus.backend.evaluation.EvaluatorSnapshot;
import com.openjiuwen.agent_evolving.evaluator.BaseEvaluator;
import com.openjiuwen.agent_evolving.evaluator.MetricEvaluator;

/** 根据 Eval 评估器版本快照创建对应的 Jiuwen evaluator。 */
public final class JiuwenEvaluatorFactory {

  public BaseEvaluator create(EvaluatorSnapshot snapshot) {
    if ("exact_match".equals(snapshot.evaluatorType())) {
      Object configured = snapshot.options().get("normalize");
      boolean normalize = configured == null || Boolean.parseBoolean(String.valueOf(configured));
      return new MetricEvaluator(new ExactAnswerMetric(normalize));
    }
    throw new UnsupportedOperationException(
        "Jiuwen evaluator type is not connected: " + snapshot.evaluatorType());
  }
}
