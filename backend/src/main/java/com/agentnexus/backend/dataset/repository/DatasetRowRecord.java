package com.agentnexus.backend.dataset.repository;

public record DatasetRowRecord(String id, Integer rowNo, java.time.LocalDateTime createdDate, java.time.LocalDateTime lastUpdatedDate) {
}
