package com.evalsystem.task.api.dto.request;

import java.util.List;

public record TaskEvaluatorInput(
    String evaluatorSource,
    String evaluatorId,
    String evaluatorVersionId,
    String modelId,
    List<TaskEvaluatorParamMappingInput> paramMappings
) {
}
