package com.evalsystem.task.api.dto.request;

public record TaskEvaluatorParamMappingInput(
    String paramId,
    String paramName,
    String sourceType,
    String datasetFieldId,
    String appOutputName
) {
}
