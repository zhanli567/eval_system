package com.agentnexus.backend.remoteCall.api.dto.response;

import java.util.List;

public record AgentUiCardDefinition(
    String id,
    String type,
    String version,
    String displayName,
    AgentUiCardLocation location,
    List<AgentUiCardComponentDefinition> body
) {
}
