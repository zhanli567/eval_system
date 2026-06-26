package com.agentnexus.backend.dataset.api.dto.request;

public record FieldInput(
    String id,
    String fieldName,
    String fieldType,
    Boolean required,
    String description
) {
}
