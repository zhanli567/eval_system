package com.evalsystem.dataset.dto;

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
