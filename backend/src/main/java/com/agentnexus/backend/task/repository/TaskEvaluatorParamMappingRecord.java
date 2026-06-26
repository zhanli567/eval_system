package com.agentnexus.backend.task.repository;

public record TaskEvaluatorParamMappingRecord(
    String id,
    String taskId,
    String taskEvaluatorId,
    String paramId,
    String paramName,
    String sourceType,
    String datasetVersionId,
    String datasetFieldId,
    String appOutputName,
    Integer displayOrder
) {
}
