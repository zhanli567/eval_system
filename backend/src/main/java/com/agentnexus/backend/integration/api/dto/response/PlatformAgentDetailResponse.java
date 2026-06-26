package com.agentnexus.backend.integration.api.dto.response;

public record PlatformAgentDetailResponse(
    String status,
    String url,
    PlatformSuperAgentDetail resultObjVO,
    Boolean success
) {
}
