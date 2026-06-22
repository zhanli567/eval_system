package com.evalsystem.task.api.dto.response;

import com.evalsystem.dataset.api.dto.response.FieldDto;
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
