package com.agentnexus.backend.task.api.dto.request;

import java.util.List;

public record CreateTaskRequest(
    String taskName,
    String description,
    String datasetId,
    String datasetVersionId,
    String appType,
    String appId,
    String appVersionId,
    String appAgentAlias,
    List<AppFieldMappingInput> appFieldMappings,
    List<TaskEvaluatorInput> evaluators,
    List<String> tagIds
) {
}
