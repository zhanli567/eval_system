package com.evalsystem.evaluator.api.dto.response;

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
