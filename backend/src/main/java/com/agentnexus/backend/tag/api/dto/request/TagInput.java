package com.agentnexus.backend.tag.api.dto.request;

import java.math.BigDecimal;
import java.util.List;

public record TagInput(
    String tagName,
    String tagType,
    String description,
    BigDecimal minValue,
    BigDecimal maxValue,
    BigDecimal passThreshold,
    List<TagOptionInput> options
) {
}
