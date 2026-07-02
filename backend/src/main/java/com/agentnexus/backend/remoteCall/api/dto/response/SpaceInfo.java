package com.agentnexus.backend.remoteCall.api.dto.response;

public record SpaceInfo(
    String id,
    String name,
    String description,
    String ownerId,
    String status,
    String memberCount,
    String createdAt,
    String updatedAt,
    String appId
) {
}
