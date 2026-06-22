package com.evalsystem.task.api.dto.response;

import com.evalsystem.tag.api.dto.response.TagOptionDto;
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
