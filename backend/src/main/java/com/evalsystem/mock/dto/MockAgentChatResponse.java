package com.evalsystem.mock.dto;

import java.util.Map;

public record MockAgentChatResponse(
    String conversationId,
    MockAgentMessage message,
    String status,
    Map<String, String> outputs,
    Long latencyMs,
    String errorMessage,
    String rawOutput
) {
}
