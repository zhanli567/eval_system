package com.agentnexus.backend.dataset.api.dto.response;

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
