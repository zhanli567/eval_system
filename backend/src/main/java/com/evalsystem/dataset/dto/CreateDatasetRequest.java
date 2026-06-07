package com.evalsystem.dataset.dto;

import java.util.List;

public record CreateDatasetRequest(
    String name,
    String description,
    List<FieldInput> fields
) {
}
