package com.agentnexus.backend.integration.api.dto.response;

public record PlatformModelListResponse(
    String status,
    String url,
    PlatformListResult<PlatformModelInfo> resultObjVO,
    Boolean success
) {
}
