package com.evalsystem.evaluator.dto;

public record EvaluatorVersionDto(
    String id,
    String evaluatorId,
    Integer versionNo,
    String versionName,
    Boolean draft,
    String createdAt,
    String updatedAt
) {
}
