package com.evalsystem.integration.dto;

public record PlatformAgentListResponse(
    String status,
    String url,
    PlatformListResult<PlatformSuperAgentInfo> resultObjVO,
    Boolean success
) {
}
