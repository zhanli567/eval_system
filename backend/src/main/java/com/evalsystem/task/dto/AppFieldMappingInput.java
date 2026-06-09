package com.evalsystem.task.dto;

public record AppFieldMappingInput(
    String appInputId,
    String appInputName,
    String appInputType,
    String datasetFieldId
) {
}
