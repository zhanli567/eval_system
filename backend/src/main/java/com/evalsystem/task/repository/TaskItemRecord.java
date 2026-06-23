package com.evalsystem.task.repository;

public record TaskItemRecord(
    String id,
    String taskId,
    String datasetVersionId,
    String datasetItemId,
    Integer rowNo,
    String status,
    String appOutput,
    String appOutputStatus,
    String appErrorMessage,
    java.time.LocalDateTime createdDate,
    java.time.LocalDateTime lastUpdatedDate
) {
}
