package com.agentnexus.backend.tag.api.dto.request;

import java.util.List;

public record TagInput(
    String tagName,
    String tagType,
    String description,
    Integer minValue,
    Integer maxValue,
    Integer passThreshold,
    List<TagOptionInput> options
) {
}
