package com.evalsystem.evaluator.dto;

public record EvaluatorParamInput(
    String id,
    String paramName,
    String dataType,
    String defaultValue
) {
}
