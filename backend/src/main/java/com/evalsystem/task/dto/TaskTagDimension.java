package com.evalsystem.task.dto;

public record TaskTagDimension(
    String taskTagId,
    String tagId,
    String tagName,
    String tagType,
    String status,
    Integer passCount,
    Integer completedCount,
    Integer totalCount,
    Double passRate,
    Integer displayOrder
) {
}
