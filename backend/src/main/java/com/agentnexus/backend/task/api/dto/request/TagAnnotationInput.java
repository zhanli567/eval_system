package com.agentnexus.backend.task.api.dto.request;

import java.math.BigDecimal;

public record TagAnnotationInput(
    String taskTagId,
    String valueText,
    BigDecimal valueNumber,
    String tagOptionId
) {
}
