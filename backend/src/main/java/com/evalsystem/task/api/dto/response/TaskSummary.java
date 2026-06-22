package com.evalsystem.task.api.dto.response;

import java.util.List;

public record TaskSummary(
    TaskBase base,
    List<TaskEvaluatorDimension> evaluators,
    List<TaskTagDimension> tags
) {
}
