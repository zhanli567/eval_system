package com.agentnexus.backend.integration.api.dto.response;

public record PlatformAgentChild(
    String agentAlias,
    String agentName,
    String version,
    String routePattern
) {
}
