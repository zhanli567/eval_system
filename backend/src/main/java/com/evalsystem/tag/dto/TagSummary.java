package com.evalsystem.tag.dto;

public record TagSummary(
    String id,
    String tagName,
    String tagType,
    String description,
    String createdAt,
    String updatedAt
) {
}
