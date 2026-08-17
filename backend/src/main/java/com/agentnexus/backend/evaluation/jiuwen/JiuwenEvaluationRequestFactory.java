package com.agentnexus.backend.evaluation.jiuwen;

import com.agentnexus.backend.evaluation.EvaluationRequest;
import com.agentnexus.backend.evaluation.EvaluatorSnapshot;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 将任务行数据和 Agent 执行结果组装为 Jiuwen 评测请求。 */
public final class JiuwenEvaluationRequestFactory {

  public EvaluationRequest create(
      String caseId,
      Map<String, String> rowValues,
      Map<String, String> outputs,
      List<Map<String, Object>> trajectory,
      Map<String, Object> metrics,
      EvaluatorSnapshot evaluator
  ) {
    Map<String, Object> caseValues = new LinkedHashMap<>();
    if (rowValues != null) {
      caseValues.putAll(rowValues);
    }
    Map<String, Object> prediction = new LinkedHashMap<>();
    if (outputs != null) {
      prediction.putAll(outputs);
    }
    return new EvaluationRequest(
        caseId,
        caseValues,
        caseValues,
        prediction,
        trajectory,
        metrics,
        evaluator);
  }
}
