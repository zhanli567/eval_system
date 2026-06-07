package com.evalsystem.dataset.dto;

public record DatasetVersionDto(
    String id,
    String datasetId,
    Integer versionNo,
    String versionName,
    Integer itemCount,
    Boolean draft,
    String createdAt,
    String updatedAt
) {
}
