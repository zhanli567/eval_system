package com.evalsystem.integration.api.dto.response;

public record PlatformModelChatResponse(
    String status,
    String url,
    PlatformModelChatResult resultObjVO,
    Boolean success
) {
}
