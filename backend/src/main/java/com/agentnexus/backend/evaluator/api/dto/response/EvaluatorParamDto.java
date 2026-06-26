package com.agentnexus.backend.evaluator.api.dto.response;

public record EvaluatorParamDto(
    String id,
    String targetType,
    String targetId,
    String paramName,
    String dataType,
    String defaultValue,
    Boolean required,
    String description,
    Integer displayOrder
) {
}
