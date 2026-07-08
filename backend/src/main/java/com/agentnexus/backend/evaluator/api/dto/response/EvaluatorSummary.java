package com.agentnexus.backend.evaluator.api.dto.response;

public record EvaluatorSummary(
    String id,
    String evaluatorName,
    String evaluatorType,
    String latestVersionId,
    Integer latestVersionNo,
    String latestVersionName,
    String description,
    String createdByName,
    java.time.LocalDateTime createdDate,
    String lastUpdatedByName,
    java.time.LocalDateTime lastUpdatedDate
) {
}
