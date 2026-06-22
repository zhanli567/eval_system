package com.evalsystem.task.repository;

public record TaskEvaluatorBindingRecord(
    String id,
    String taskId,
    String evaluatorSource,
    String evaluatorId,
    String evaluatorVersionId,
    String modelId,
    String status,
    Integer displayOrder
) {
}
