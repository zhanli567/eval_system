package com.evalsystem.evaluator.dto;

public record PresetEvaluatorSummary(
    String id,
    String categoryId,
    String categoryName,
    String evaluatorName,
    String evaluatorType,
    String description
) {
}
