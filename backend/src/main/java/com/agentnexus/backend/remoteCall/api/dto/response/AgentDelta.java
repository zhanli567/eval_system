package com.agentnexus.backend.remoteCall.api.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public record AgentDelta(
    String role,
    List<AgentContentBlock> content,
    @JsonProperty("tool_calls") List<AgentToolCallDelta> toolCalls,
    Map<String, Object> extra
) {
}
