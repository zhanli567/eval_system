package com.agentnexus.backend.remoteCall.api.dto.response;

public record PlatformAgentChild(
    String agentAlias,
    String agentName,
    String version,
    String routePattern
) {
}
