package com.evalsystem.evaluator.api.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record EvaluatorConfig(
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
    String updatedAt,
    List<EvaluatorParamDto> params
) {
}
