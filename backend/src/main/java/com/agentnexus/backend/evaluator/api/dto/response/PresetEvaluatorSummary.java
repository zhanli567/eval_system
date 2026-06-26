package com.agentnexus.backend.evaluator.api.dto.response;

public record PresetEvaluatorSummary(
    String id,
    String categoryId,
    String categoryName,
    String evaluatorName,
    String evaluatorType,
    String description
) {
}
