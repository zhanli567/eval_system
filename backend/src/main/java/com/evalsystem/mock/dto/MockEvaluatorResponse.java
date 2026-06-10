package com.evalsystem.mock.dto;

import java.math.BigDecimal;

public record MockEvaluatorResponse(
    String status,
    BigDecimal score,
    String reason,
    String errorMessage,
    String rawOutput,
    Long latencyMs
) {
}
