package com.agentnexus.backend.task.api.dto.response;

import com.agentnexus.backend.common.PageResponse;
import com.agentnexus.backend.dataset.api.dto.response.FieldDto;
import java.util.List;

public record TaskDetail(
    TaskBase base,
    List<FieldDto> fields,
    List<TaskEvaluatorDimension> evaluators,
    List<TaskTagDimension> tags,
    PageResponse<TaskItemDetail> items
) {
}
