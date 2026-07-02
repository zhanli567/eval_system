package com.agentnexus.backend.remoteCall.api.dto.response;

public record PlatformAgentToolCallDelta(
    Integer index,
    String id,
    String type,
    PlatformAgentFunctionDelta function
) {
}
