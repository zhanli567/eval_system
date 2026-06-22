package com.evalsystem.integration.api.dto.response;

public record PlatformAgentListResponse(
    String status,
    String url,
    PlatformListResult<PlatformSuperAgentInfo> resultObjVO,
    Boolean success
) {
}
