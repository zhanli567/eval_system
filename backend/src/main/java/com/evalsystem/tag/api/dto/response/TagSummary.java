package com.evalsystem.tag.api.dto.response;

public record TagSummary(
    String id,
    String tagName,
    String tagType,
    String description,
    java.time.LocalDateTime createdDate,
    java.time.LocalDateTime lastUpdatedDate
) {
}
