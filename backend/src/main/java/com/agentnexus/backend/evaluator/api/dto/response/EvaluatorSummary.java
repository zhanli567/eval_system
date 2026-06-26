package com.agentnexus.backend.evaluator.api.dto.response;

public record EvaluatorSummary(
    String id,
    String evaluatorName,
    String evaluatorType,
    String latestVersionId,
    Integer latestVersionNo,
    String latestVersionName,
    String description,
    java.time.LocalDateTime createdDate,
    java.time.LocalDateTime lastUpdatedDate
) {
}
