package com.agentnexus.backend.evaluator.api.dto.response;

public record EvaluatorVersionDto(
    String id,
    String evaluatorId,
    Integer versionNo,
    String versionName,
    Boolean draft,
    java.time.LocalDateTime createdDate,
    java.time.LocalDateTime lastUpdatedDate
) {
}
