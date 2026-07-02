package com.agentnexus.backend.remoteCall.api.dto.response;

public record AgentToolCallDelta(
    Integer index,
    String id,
    String type,
    AgentFunctionDelta function
) {
}
