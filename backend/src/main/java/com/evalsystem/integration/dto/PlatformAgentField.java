package com.evalsystem.integration.dto;

public record PlatformAgentField(
    String id,
    String fieldName,
    String fieldType,
    String description,
    Integer displayOrder
) {
}
