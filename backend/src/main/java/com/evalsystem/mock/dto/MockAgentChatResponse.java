package com.evalsystem.mock.dto;

import java.util.List;
import java.util.Map;

public record MockAgentChatResponse(
    String id,
    String conversationId,
    String masterAgent,
    String metaAgent,
    String object,
    Long created,
    String nmodel,
    List<MockAgentChoice> choices,
    String status,
    Map<String, String> outputs,
    Long latencyMs,
    String errorMessage,
    String rawOutput
) {
}
