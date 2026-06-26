package com.agentnexus.backend.integration.api.dto.response;

public record PlatformAgentField(
    String id,
    String fieldName,
    String fieldType,
    String description,
    Integer displayOrder
) {
}
