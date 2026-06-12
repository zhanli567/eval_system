package com.evalsystem.integration.dto;

public record PlatformModelListResponse(
    String status,
    String url,
    PlatformListResult<PlatformModelInfo> resultObjVO,
    Boolean success
) {
}
