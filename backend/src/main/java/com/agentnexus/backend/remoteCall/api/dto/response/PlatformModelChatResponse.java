package com.agentnexus.backend.remoteCall.api.dto.response;

public record PlatformModelChatResponse(
    String status,
    String url,
    PlatformModelChatResult resultObjVO,
    Boolean success
) {
}
