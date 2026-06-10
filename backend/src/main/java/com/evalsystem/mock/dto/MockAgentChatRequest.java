package com.evalsystem.mock.dto;

import java.util.List;

public record MockAgentChatRequest(
    String conversationId,
    List<MockAgentMessage> message,
    Boolean stream
) {
}
