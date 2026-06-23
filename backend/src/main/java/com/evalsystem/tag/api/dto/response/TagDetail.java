package com.evalsystem.tag.api.dto.response;

import java.util.List;

public record TagDetail(
    String id,
    String tagName,
    String tagType,
    String description,
    Integer minValue,
    Integer maxValue,
    Integer passThreshold,
    String createdAt,
    java.time.LocalDateTime lastUpdatedDate,
    List<TagOptionDto> options
) {
}
