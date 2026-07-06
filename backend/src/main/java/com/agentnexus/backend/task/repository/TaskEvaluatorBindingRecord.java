package com.agentnexus.backend.task.repository;

public record TaskEvaluatorBindingRecord(
    String id,
    String taskId,
    String evaluatorSource,
    String evaluatorId,
    String evaluatorVersionId,
    String modelId,
    String modelName,
    String status,
    Integer displayOrder
) {
}
