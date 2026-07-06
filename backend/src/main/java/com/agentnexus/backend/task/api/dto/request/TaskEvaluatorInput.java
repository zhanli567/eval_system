package com.agentnexus.backend.task.api.dto.request;

import java.util.List;

public record TaskEvaluatorInput(
    String evaluatorSource,
    String evaluatorId,
    String evaluatorVersionId,
    String modelId,
    String modelName,
    List<TaskEvaluatorParamMappingInput> paramMappings
) {
}
