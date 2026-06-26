package com.agentnexus.backend.integration.api.dto.response;

public record PlatformModelChatResponse(
    String status,
    String url,
    PlatformModelChatResult resultObjVO,
    Boolean success
) {
}
