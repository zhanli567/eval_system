package com.evalsystem.task.api.dto.request;

import java.util.List;

public record SaveAnnotationRequest(
    List<TagAnnotationInput> tags
) {
}
