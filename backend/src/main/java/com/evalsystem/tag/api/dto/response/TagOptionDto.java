package com.evalsystem.tag.api.dto.response;

public record TagOptionDto(
    String id,
    String tagId,
    String optionName,
    String optionGroup,
    Integer displayOrder,
    java.time.LocalDateTime createdDate,
    java.time.LocalDateTime lastUpdatedDate
) {
}
