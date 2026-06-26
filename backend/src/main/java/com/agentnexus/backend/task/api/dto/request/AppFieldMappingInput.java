package com.agentnexus.backend.task.api.dto.request;

public record AppFieldMappingInput(
    String appInputId,
    String appInputName,
    String appInputType,
    String datasetFieldId
) {
}
