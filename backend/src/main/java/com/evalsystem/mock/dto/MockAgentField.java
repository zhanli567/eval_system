package com.evalsystem.mock.dto;

public record MockAgentField(
    String id,
    String fieldName,
    String fieldType,
    String description,
    Integer displayOrder
) {
}
