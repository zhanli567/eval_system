package com.agentnexus.backend.integration.api.dto.response;

import java.util.Map;

public record PlatformAgentUiCardComponentDefinition(
    String id,
    String type,
    String componentKey,
    Map<String, Object> propsData
) {
}
