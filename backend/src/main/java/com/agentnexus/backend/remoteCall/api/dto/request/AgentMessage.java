package com.agentnexus.backend.remoteCall.api.dto.request;

public record AgentMessage(
    String role,
    String content
) {
}
