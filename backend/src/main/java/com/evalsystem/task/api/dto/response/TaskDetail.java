package com.evalsystem.task.api.dto.response;

import com.evalsystem.common.PageResponse;
import com.evalsystem.dataset.api.dto.response.FieldDto;
import java.util.List;

public record TaskDetail(
    TaskBase base,
    List<FieldDto> fields,
    List<TaskEvaluatorDimension> evaluators,
    List<TaskTagDimension> tags,
    PageResponse<TaskItemDetail> items
) {
}
