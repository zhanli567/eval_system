package com.agentnexus.backend.remoteCall.api.dto.response;

import java.util.List;
import java.util.Map;

public record AgentChatResponse(
    String id,
    String conversationId,
    Object masterAgent,
    Object metaAgent,
    String userId,
    String object,
    Long created,
    String model,
    List<AgentChoice> choices,
    String status,
    Map<String, String> outputs,
    Long latencyMs,
    String errorMessage,
    String rawOutput
) {
}
