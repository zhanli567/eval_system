package com.agentnexus.backend.remoteCall.api.dto.response;

public record PlatformAgentReferenceItem(
    String id,
    String title,
    String url,
    PlatformAgentReferenceSourceType sourceType,
    String sourceName,
    String summary,
    String snippet
) {
}
