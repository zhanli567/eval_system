package com.evalsystem.integration.dto;

public record PlatformModelChatResult(
    String modelId,
    String outputText,
    String checkedAt
) {
}
