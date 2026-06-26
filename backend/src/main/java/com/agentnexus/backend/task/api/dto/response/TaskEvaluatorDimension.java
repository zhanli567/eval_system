package com.agentnexus.backend.task.api.dto.response;

public record TaskEvaluatorDimension(
    String taskEvaluatorId,
    String evaluatorSource,
    String evaluatorId,
    String evaluatorVersionId,
    String evaluatorName,
    String evaluatorType,
    String versionName,
    String status,
    Integer passCount,
    Integer completedCount,
    Integer totalCount,
    Double passRate,
    Integer displayOrder
) {
}
