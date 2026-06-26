package com.agentnexus.backend.evaluator.api.dto.request;

public record EvaluatorParamInput(
    String id,
    String paramName,
    String dataType,
    String defaultValue,
    Boolean required,
    String description
) {
}
