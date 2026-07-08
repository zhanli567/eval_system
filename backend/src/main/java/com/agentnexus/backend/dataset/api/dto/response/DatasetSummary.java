package com.agentnexus.backend.dataset.api.dto.response;

public record DatasetSummary(
    String id,
    String name,
    String description,
    Integer publishedVersionCount,
    String latestPublishedVersionId,
    Integer latestItemCount,
    String createdByName,
    java.time.LocalDateTime createdDate,
    String lastUpdatedByName,
    java.time.LocalDateTime lastUpdatedDate
) {
}
