package com.evalsystem.dataset.api.dto.response;

import java.util.Map;

public record RowDto(
    String id,
    Integer rowNo,
    Map<String, String> values,
    String createdAt,
    String updatedAt
) {
}
