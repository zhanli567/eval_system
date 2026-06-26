package com.agentnexus.backend.task.api.dto.response;

import com.agentnexus.backend.tag.api.dto.response.TagOptionDto;
import java.util.List;

public record TaskTagAnnotation(
    String taskTagId,
    String tagId,
    String tagName,
    String tagType,
    String description,
    Integer minValue,
    Integer maxValue,
    Integer passThreshold,
    List<TagOptionDto> options,
    TaskTagResultDto result
) {
}
