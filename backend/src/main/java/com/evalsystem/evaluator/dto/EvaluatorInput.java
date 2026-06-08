package com.evalsystem.evaluator.dto;

import java.math.BigDecimal;
import java.util.List;

public record EvaluatorInput(
    String evaluatorName,
    String evaluatorType,
    String description,
    String modelId,
    String prompt,
    String executeCode,
    BigDecimal scoreMin,
    BigDecimal scoreMax,
    BigDecimal passThreshold,
    List<EvaluatorParamInput> params
) {
}
