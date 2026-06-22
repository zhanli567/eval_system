package com.evalsystem.task.repository;

public record TaskAppFieldMappingRecord(
    String id,
    String taskId,
    String appInputId,
    String appInputName,
    String appInputType,
    String datasetVersionId,
    String datasetFieldId,
    Integer displayOrder
) {
}
