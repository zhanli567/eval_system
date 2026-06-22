package com.evalsystem.task.api.dto.response;

import java.math.BigDecimal;

public record TaskEvaluatorResultDto(
    String id,
    String taskItemId,
    String taskEvaluatorId,
    String evaluatorName,
    String evaluatorType,
    String versionName,
    String status,
    BigDecimal score,
    String passResult,
    String resultValue,
    String errorMessage,
    String startedAt,
    String finishedAt
) {
}
