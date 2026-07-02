package com.agentnexus.backend.remoteCall.api.dto.response;

import java.util.List;

public record ModelInfo(
    String modelId,
    String name,
    String provider,
    String modelName,
    String baseUrlRef,
    String timeoutPolicy,
    List<String> capabilities,
    String authType,
    String status,
    String createAt,
    String updateAt
) {
}
