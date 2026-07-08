package com.agentnexus.backend.tag.api.dto.response;

public record TagSummary(
    String id,
    String tagName,
    String tagType,
    String description,
    String createdByName,
    java.time.LocalDateTime createdDate,
    String lastUpdatedByName,
    java.time.LocalDateTime lastUpdatedDate
) {
}
