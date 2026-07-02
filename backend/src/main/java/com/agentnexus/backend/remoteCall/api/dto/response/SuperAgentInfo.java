package com.agentnexus.backend.remoteCall.api.dto.response;

import java.util.Map;

public record SuperAgentInfo(
    String superAgentId,
    String name,
    String displayName,
    String publishStatus,
    Integer onlineInstances,
    Integer totalInstances,
    String bundleVersion,
    String bundleDigest,
    String syncStatus,
    Map<String, Object> metrics,
    Integer loadedAgentCount,
    String spaceId,
    String currentBundleId,
    String description,
    String iconUrl
) {
}
