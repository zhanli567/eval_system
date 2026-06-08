package com.evalsystem.tag.dto;

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
    String updatedAt,
    List<TagOptionDto> options
) {
}
