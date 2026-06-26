package com.agentnexus.backend.tag.api.dto.response;

public record TagConfig(
    String id,
    String tagName,
    String tagType,
    String description,
    Integer minValue,
    Integer maxValue,
    Integer passThreshold,
    java.time.LocalDateTime createdDate,
    java.time.LocalDateTime lastUpdatedDate
) {
}
