package com.agentnexus.backend.dataset.api.dto.request;

import java.util.List;
import java.util.Map;

public record BatchRowsRequest(
    List<Map<String, String>> rows
) {
}
