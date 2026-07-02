package com.agentnexus.backend.remoteCall.api.dto.request;

import java.util.List;

public record PlatformAgentChatRequest(
    String conversationId,
    List<PlatformAgentMessage> messages,
    Boolean stream
) {
}
