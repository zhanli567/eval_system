package com.evalsystem.task.dto;

import java.math.BigDecimal;

public record TaskTagResultDto(
    String id,
    String taskItemId,
    String taskTagId,
    String tagId,
    String tagName,
    String tagType,
    String status,
    String valueText,
    BigDecimal valueNumber,
    String tagOptionId,
    String optionName,
    String passResult,
    String annotatedAt
) {
}
