package com.evalsystem.task.dto;

import java.util.List;

public record TaskSummary(
    TaskBase base,
    List<TaskEvaluatorDimension> evaluators,
    List<TaskTagDimension> tags
) {
}
