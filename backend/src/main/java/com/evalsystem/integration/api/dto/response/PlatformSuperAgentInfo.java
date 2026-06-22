package com.evalsystem.integration.api.dto.response;

import java.util.Map;

public record PlatformSuperAgentInfo(
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
    String description
) {
}
