package com.evalsystem.integration.api.dto.response;

import java.util.List;

public record PlatformAgentUiCardDefinition(
    String id,
    String type,
    String version,
    String displayName,
    PlatformAgentUiCardLocation location,
    List<PlatformAgentUiCardComponentDefinition> body
) {
}
