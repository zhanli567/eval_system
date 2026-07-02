package com.agentnexus.backend.remoteCall.api.dto.response;

public record PlatformAgentField(
    String id,
    String fieldName,
    String fieldType,
    String description,
    Integer displayOrder
) {
}
