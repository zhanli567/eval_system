package com.evalsystem.integration.api.dto.response;

public record PlatformAgentContentBlock(
    String type,
    String text,
    String reasoning,
    String error
) {
}
