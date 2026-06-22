package com.evalsystem.integration.api.dto.response;

import java.util.List;

public record PlatformAgentDefinition(
    String id,
    String agentName,
    String description,
    List<PlatformAgentVersion> versions,
    List<PlatformAgentField> inputs,
    List<PlatformAgentField> outputs
) {
}
