package com.agentnexus.backend.remoteCall.api.dto.response;

public record ModelChatResult(
    String modelId,
    String outputText,
    String checkedAt
) {
}
