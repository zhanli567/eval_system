package com.agentnexus.backend.evaluator.api.dto.response;

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
    String modelName,
    String prompt,
    String executeCode,
    BigDecimal scoreMin,
    BigDecimal scoreMax,
    BigDecimal passThreshold,
    java.time.LocalDateTime createdDate,
    java.time.LocalDateTime lastUpdatedDate
) {
}
