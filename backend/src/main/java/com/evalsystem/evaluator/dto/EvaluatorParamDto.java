package com.evalsystem.evaluator.dto;

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
