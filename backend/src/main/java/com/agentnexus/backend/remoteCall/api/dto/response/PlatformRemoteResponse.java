package com.agentnexus.backend.remoteCall.api.dto.response;

public record PlatformRemoteResponse<T>(
    String status,
    String url,
    T resultObjVO,
    Boolean success
) {
}
