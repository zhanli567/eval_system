package com.agentnexus.backend.integration.api.dto.response;

import java.util.List;

public record PlatformAgentDefinition(
    String id,
    String agentName,
    String description,
    String iconUrl,
    List<PlatformAgentVersion> versions,
    List<PlatformAgentChild> childAgents,
    List<PlatformAgentField> inputs,
    List<PlatformAgentField> outputs
) {
}
