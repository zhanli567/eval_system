package com.agentnexus.backend.integration.api.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public record PlatformAgentDelta(
    String role,
    List<PlatformAgentContentBlock> content,
    @JsonProperty("tool_calls") List<PlatformAgentToolCallDelta> toolCalls,
    Map<String, Object> extra
) {
}
