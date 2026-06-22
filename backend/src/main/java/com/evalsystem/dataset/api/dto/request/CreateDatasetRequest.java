package com.evalsystem.dataset.api.dto.request;

import java.util.List;

public record CreateDatasetRequest(
    String name,
    String description,
    List<FieldInput> fields
) {
}
