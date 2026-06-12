package com.evalsystem.integration.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public record PlatformAgentDelta(
    String role,
    List<PlatformAgentContentBlock> content,
    @JsonProperty("tool_calls") List<Map<String, Object>> toolCalls,
    Map<String, Object> extra
) {
}
