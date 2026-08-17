package com.agentnexus.backend.evaluation.jiuwen;

import com.openjiuwen.agent_evolving.evaluator.metrics.ExactMatchMetric;
import com.openjiuwen.agent_evolving.evaluator.metrics.Metric;
import java.util.Map;

/** 让 Jiuwen 精确匹配评估器只比较结构化 prediction 中的 answer 字段。 */
public final class ExactAnswerMetric extends Metric {
  private final ExactMatchMetric delegate;

  public ExactAnswerMetric(boolean normalize) {
    this.delegate = new ExactMatchMetric(normalize);
  }

  @Override
  public String getName() {
    return "exact_match";
  }

  @Override
  public Object compute(Object prediction, Object label, Map<String, Object> kwargs) {
    return delegate.compute(answerValue(prediction), answerValue(label), kwargs);
  }

  private Object answerValue(Object value) {
    if (value instanceof Map<?, ?> map && map.containsKey("answer")) {
      return map.get("answer");
    }
    return value;
  }
}
