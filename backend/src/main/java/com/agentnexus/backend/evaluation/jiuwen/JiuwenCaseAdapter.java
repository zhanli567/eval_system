package com.agentnexus.backend.evaluation.jiuwen;

import com.agentnexus.backend.evaluation.EvaluationRequest;
import com.openjiuwen.agent_evolving.dataset.Case;
import java.util.LinkedHashMap;
import java.util.Map;

/** 将 Eval-System 的一次评测请求转换为 Jiuwen 数据模型。 */
public final class JiuwenCaseAdapter {

  public JiuwenCaseInput adapt(EvaluationRequest request) {
    Case caseValue = new Case(request.inputs(), request.labels(), null, request.caseId());
    Map<String, Object> predict = new LinkedHashMap<>(request.prediction());
    predict.put("outputs", request.prediction());
    predict.put("trajectory", request.trajectory());
    predict.put("metrics", request.metrics());
    return new JiuwenCaseInput(caseValue, predict);
  }
}
