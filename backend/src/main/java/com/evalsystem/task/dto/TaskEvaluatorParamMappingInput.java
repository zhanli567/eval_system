package com.evalsystem.task.dto;

public record TaskEvaluatorParamMappingInput(
    String paramId,
    String paramName,
    String sourceType,
    String datasetFieldId,
    String appOutputName
) {
}
