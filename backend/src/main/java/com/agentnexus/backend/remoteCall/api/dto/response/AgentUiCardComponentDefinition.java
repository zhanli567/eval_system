package com.agentnexus.backend.remoteCall.api.dto.response;

import java.util.Map;

public record AgentUiCardComponentDefinition(
    String id,
    String type,
    String componentKey,
    Map<String, Object> propsData
) {
}
