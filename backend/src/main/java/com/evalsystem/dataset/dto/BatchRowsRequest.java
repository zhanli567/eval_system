package com.evalsystem.dataset.dto;

import java.util.List;
import java.util.Map;

public record BatchRowsRequest(
    List<Map<String, String>> rows
) {
}
