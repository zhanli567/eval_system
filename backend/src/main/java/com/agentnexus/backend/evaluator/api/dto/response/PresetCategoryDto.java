package com.agentnexus.backend.evaluator.api.dto.response;

public record PresetCategoryDto(
    String id,
    String categoryName,
    Integer displayOrder
) {
}
