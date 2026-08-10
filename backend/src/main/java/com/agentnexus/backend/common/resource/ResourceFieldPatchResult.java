package com.agentnexus.backend.common.resource;

import java.util.Map;

public record ResourceFieldPatchResult(
    String resourceType,
    String resourceId,
    Map<String, Object> fields
) {
}
