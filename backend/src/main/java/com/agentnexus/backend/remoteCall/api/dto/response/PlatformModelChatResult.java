package com.agentnexus.backend.remoteCall.api.dto.response;

public record PlatformModelChatResult(
    String modelId,
    String outputText,
    String checkedAt
) {
}
