package com.evalsystem.task.dto;

import com.evalsystem.dataset.dto.FieldDto;
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
