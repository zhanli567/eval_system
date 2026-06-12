package com.evalsystem.integration.dto;

public record PlatformModelChatResponse(
    String status,
    String url,
    PlatformModelChatResult resultObjVO,
    Boolean success
) {
}
