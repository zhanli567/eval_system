package com.evalsystem.mock.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public record MockAgentDelta(
    String role,
    List<MockAgentDeltaContent> content,
    @JsonProperty("tool_calls") List<Map<String, Object>> toolCalls,
    Map<String, Object> extra
) {
}
