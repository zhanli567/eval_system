package com.agentnexus.backend.remoteCall.api.dto.response;

public record AgentField(
    String id,
    String fieldName,
    String fieldType,
    String description,
    Integer displayOrder
) {
}
