package com.evalsystem.dataset.dto;

import java.util.Map;

public record RowInput(
    String id,
    Map<String, String> values
) {
}
