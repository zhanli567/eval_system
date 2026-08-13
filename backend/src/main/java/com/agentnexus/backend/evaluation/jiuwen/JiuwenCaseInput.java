package com.agentnexus.backend.evaluation.jiuwen;

import com.openjiuwen.agent_evolving.dataset.Case;
import java.util.Map;

/** 传给 Jiuwen evaluator 的标准 Case 和预测输出。 */
public record JiuwenCaseInput(Case caseValue, Map<String, Object> predict) {
  public JiuwenCaseInput {
    predict = Map.copyOf(predict);
  }
}
