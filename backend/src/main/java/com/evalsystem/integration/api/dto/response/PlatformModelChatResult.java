package com.evalsystem.integration.api.dto.response;

public record PlatformModelChatResult(
    String modelId,
    String outputText,
    String checkedAt
) {
}
