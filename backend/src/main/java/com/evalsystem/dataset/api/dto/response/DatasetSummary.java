package com.evalsystem.dataset.api.dto.response;

public record DatasetSummary(
    String id,
    String name,
    String description,
    Integer publishedVersionCount,
    String latestPublishedVersionId,
    Integer latestItemCount,
    java.time.LocalDateTime createdDate,
    java.time.LocalDateTime lastUpdatedDate
) {
}
