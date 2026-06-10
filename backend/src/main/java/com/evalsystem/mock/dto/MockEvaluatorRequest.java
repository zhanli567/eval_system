package com.evalsystem.mock.dto;

import java.math.BigDecimal;
import java.util.Map;

public record MockEvaluatorRequest(
    String taskId,
    String taskItemId,
    String taskEvaluatorId,
    String evaluatorName,
    String evaluatorType,
    String modelId,
    String promptTemplate,
    String renderedPrompt,
    String executeCode,
    BigDecimal scoreMin,
    BigDecimal scoreMax,
    BigDecimal passThreshold,
    Map<String, Object> params
) {
}
