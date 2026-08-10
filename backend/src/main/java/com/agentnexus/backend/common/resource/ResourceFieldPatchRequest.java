package com.agentnexus.backend.common.resource;

import java.util.Map;

public record ResourceFieldPatchRequest(Map<String, Object> fields) {
}
