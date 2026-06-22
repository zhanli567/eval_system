package com.evalsystem.task.repository;

public record TaskTagBindingRecord(
    String id,
    String taskId,
    String tagId,
    String tagName,
    String tagType,
    String description,
    Integer minValue,
    Integer maxValue,
    Integer passThreshold,
    String status,
    Integer displayOrder
) {
}
