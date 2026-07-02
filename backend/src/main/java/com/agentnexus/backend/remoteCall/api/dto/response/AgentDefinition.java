package com.agentnexus.backend.remoteCall.api.dto.response;

import java.util.List;

public record AgentDefinition(
    String id,
    String agentName,
    String description,
    String iconUrl,
    List<AgentVersion> versions,
    List<AgentChild> childAgents,
    List<AgentField> inputs,
    List<AgentField> outputs
) {
}
