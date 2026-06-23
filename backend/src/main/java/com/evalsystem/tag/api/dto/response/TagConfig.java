package com.evalsystem.tag.api.dto.response;

public record TagConfig(
    String id,
    String tagName,
    String tagType,
    String description,
    Integer minValue,
    Integer maxValue,
    Integer passThreshold,
    String createdAt,
    java.time.LocalDateTime lastUpdatedDate
) {
}
