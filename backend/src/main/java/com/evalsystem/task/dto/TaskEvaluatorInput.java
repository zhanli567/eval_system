package com.evalsystem.task.dto;

import java.util.List;

public record TaskEvaluatorInput(
    String evaluatorSource,
    String evaluatorId,
    String evaluatorVersionId,
    List<TaskEvaluatorParamMappingInput> paramMappings
) {
}
