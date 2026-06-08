package com.evalsystem.evaluator.dto;

public record EvaluatorSummary(
    String id,
    String evaluatorName,
    String evaluatorType,
    String latestVersionId,
    Integer latestVersionNo,
    String latestVersionName,
    String description,
    String createdAt,
    String updatedAt
) {
}
