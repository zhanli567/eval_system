package com.agentnexus.backend.dataset.api.dto.request;

import java.util.Map;

public record RowInput(
    String id,
    Map<String, String> values
) {
}
