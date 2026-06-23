package com.evalsystem.evaluator.api.dto.response;

import java.math.BigDecimal;

public record EvaluatorConfigBase(
    String evaluatorId,
    String evaluatorName,
    String evaluatorType,
    String description,
    String versionId,
    Integer versionNo,
    String versionName,
    Boolean draft,
    String modelId,
    String prompt,
    String executeCode,
    BigDecimal scoreMin,
    BigDecimal scoreMax,
    BigDecimal passThreshold,
    String createdAt,
    java.time.LocalDateTime lastUpdatedDate
) {
}
