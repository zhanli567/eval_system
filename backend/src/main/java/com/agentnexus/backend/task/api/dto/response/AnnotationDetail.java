package com.agentnexus.backend.task.api.dto.response;

import com.agentnexus.backend.dataset.api.dto.response.FieldDto;
import java.util.List;

public record AnnotationDetail(
    TaskBase task,
    TaskItemDetail item,
    List<FieldDto> fields,
    List<TaskTagAnnotation> tags,
    List<TaskEvaluatorResultDto> evaluators,
    String previousItemId,
    String nextItemId
) {
}
