package com.evalsystem.integration.api.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PlatformLoadedAgent(
    String version,
    String metaAgentName,
    String agentAlias,
    Integer executionOrder,
    String routePattern
) {
}
