package com.evalsystem.dataset.api.dto.response;

public record DatasetSummary(
    String id,
    String name,
    String description,
    Integer publishedVersionCount,
    String latestPublishedVersionId,
    Integer latestItemCount,
    String createdAt,
    String updatedAt
) {
}
