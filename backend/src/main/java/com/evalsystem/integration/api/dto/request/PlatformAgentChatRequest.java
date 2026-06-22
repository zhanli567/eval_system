package com.evalsystem.integration.api.dto.request;

import java.util.List;

public record PlatformAgentChatRequest(
    String conversationId,
    List<PlatformAgentMessage> messages,
    Boolean stream
) {
}
