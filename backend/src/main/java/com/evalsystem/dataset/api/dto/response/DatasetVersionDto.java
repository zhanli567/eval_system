package com.evalsystem.dataset.api.dto.response;

public record DatasetVersionDto(
    String id,
    String datasetId,
    Integer versionNo,
    String versionName,
    Integer itemCount,
    Boolean draft,
    String createdAt,
    java.time.LocalDateTime lastUpdatedDate
) {
}
