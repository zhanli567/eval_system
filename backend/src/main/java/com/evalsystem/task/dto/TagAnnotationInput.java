package com.evalsystem.task.dto;

import java.math.BigDecimal;

public record TagAnnotationInput(
    String taskTagId,
    String valueText,
    BigDecimal valueNumber,
    String tagOptionId
) {
}
