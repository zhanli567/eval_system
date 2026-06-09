package com.evalsystem.task.dto;

import java.util.List;

public record SaveAnnotationRequest(
    List<TagAnnotationInput> tags
) {
}
