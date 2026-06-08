package com.evalsystem.tag.dto;

public record TagOptionDto(
    String id,
    String tagId,
    String optionName,
    String optionGroup,
    Integer displayOrder,
    String createdAt,
    String updatedAt
) {
}
