package com.evalsystem.evaluator.api.dto.response;

public record EvaluatorVersionDto(
    String id,
    String evaluatorId,
    Integer versionNo,
    String versionName,
    Boolean draft,
    String createdAt,
    java.time.LocalDateTime lastUpdatedDate
) {
}
