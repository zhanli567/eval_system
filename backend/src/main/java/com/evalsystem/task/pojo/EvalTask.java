package com.evalsystem.task.pojo;

public record EvalTask(
    String id,
    String taskName,
    String status,
    String description,
    String datasetId,
    String datasetVersionId,
    Integer itemCount,
    String appType,
    String appId,
    String appVersionId,
    String startedAt,
    String finishedAt,
    Integer isDeleted,
    String createdAt,
    String updatedAt
) {
}
