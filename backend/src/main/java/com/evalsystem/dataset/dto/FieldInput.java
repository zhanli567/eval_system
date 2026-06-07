package com.evalsystem.dataset.dto;

public record FieldInput(
    String id,
    String fieldName,
    String fieldType,
    Boolean required,
    String description
) {
}
