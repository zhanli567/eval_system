package com.evalsystem.integration.dto;

import java.util.List;

public record PlatformAgentChatRequest(
    String conversationId,
    List<PlatformAgentMessage> messages,
    Boolean stream
) {
}
