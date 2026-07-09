package com.agentnexus.backend.dataset.api.dto.response;

public record DatasetVersionDto(
    String id,
    String datasetId,
    Integer versionNo,
    String versionName,
    Integer itemCount,
    Boolean draft,
    String createdByName,
    java.time.LocalDateTime createdDate,
    java.time.LocalDateTime lastUpdatedDate
) {
}
