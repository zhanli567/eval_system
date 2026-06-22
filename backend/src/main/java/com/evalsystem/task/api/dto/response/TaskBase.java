package com.evalsystem.task.api.dto.response;

public record TaskBase(
    String id,
    String taskName,
    String status,
    String description,
    String datasetId,
    String datasetName,
    String datasetVersionId,
    Integer datasetVersionNo,
    String datasetVersionName,
    Integer itemCount,
    String appType,
    String appId,
    String appVersionId,
    String appAgentAlias,
    String startedAt,
    String finishedAt,
    String createdAt,
    String updatedAt
) {
}
