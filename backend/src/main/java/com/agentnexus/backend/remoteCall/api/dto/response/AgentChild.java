package com.agentnexus.backend.remoteCall.api.dto.response;

public record AgentChild(
    String agentAlias,
    String agentName,
    String version,
    String routePattern
) {
}
