package com.evalsystem.task.dto;

import com.evalsystem.common.PageResponse;
import com.evalsystem.dataset.dto.FieldDto;
import java.util.List;

public record TaskDetail(
    TaskBase base,
    List<FieldDto> fields,
    List<TaskEvaluatorDimension> evaluators,
    List<TaskTagDimension> tags,
    PageResponse<TaskItemDetail> items
) {
}
