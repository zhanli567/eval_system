package com.evalsystem.evaluator.dto;

import java.math.BigDecimal;

public record PresetEvaluatorConfig(
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
    String updatedAt
) {
}
