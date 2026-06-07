package com.evalsystem.dataset.dto;

public record FieldDto(
    String id,
    String versionId,
    String fieldName,
    String fieldType,
    Boolean required,
    String description,
    Integer displayOrder
) {
}
