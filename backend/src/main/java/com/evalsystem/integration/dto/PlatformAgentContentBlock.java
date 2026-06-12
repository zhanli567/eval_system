package com.evalsystem.integration.dto;

public record PlatformAgentContentBlock(
    String type,
    String text,
    String reasoning,
    String error
) {
}
