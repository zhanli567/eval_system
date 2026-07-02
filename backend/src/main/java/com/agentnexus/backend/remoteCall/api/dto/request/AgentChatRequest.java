package com.agentnexus.backend.remoteCall.api.dto.request;

import java.util.List;

public record AgentChatRequest(
    String conversationId,
    List<AgentMessage> messages,
    Boolean stream
) {
}
