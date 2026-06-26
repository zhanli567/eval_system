package com.agentnexus.backend.integration.api.dto.response;

public record PlatformAgentBundleListResponse(
    String status,
    String url,
    PlatformAgentBundleListResult resultObjVO,
    Boolean success
) {
}
