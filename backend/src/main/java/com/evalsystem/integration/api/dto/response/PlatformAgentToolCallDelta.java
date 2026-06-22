package com.evalsystem.integration.api.dto.response;

public record PlatformAgentToolCallDelta(
    Integer index,
    String id,
    String type,
    PlatformAgentFunctionDelta function
) {
}
