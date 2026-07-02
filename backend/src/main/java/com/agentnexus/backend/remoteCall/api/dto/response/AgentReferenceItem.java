package com.agentnexus.backend.remoteCall.api.dto.response;

public record AgentReferenceItem(
    String id,
    String title,
    String url,
    AgentReferenceSourceType sourceType,
    String sourceName,
    String summary,
    String snippet
) {
}
