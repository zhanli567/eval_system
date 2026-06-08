package com.evalsystem.evaluator.dto;

import java.math.BigDecimal;
import java.util.List;

public record PresetEvaluatorDetail(
    String id,
    String categoryId,
    String categoryName,
    String evaluatorName,
    String evaluatorType,
    String description,
    String modelId,
    String prompt,
    String executeCode,
    BigDecimal scoreMin,
    BigDecimal scoreMax,
    BigDecimal passThreshold,
    String createdAt,
    String updatedAt,
    List<EvaluatorParamDto> params
) {
}
