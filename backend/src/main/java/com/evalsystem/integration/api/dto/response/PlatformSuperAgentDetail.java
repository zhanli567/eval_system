package com.evalsystem.integration.api.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PlatformSuperAgentDetail(
    String superAgentId,
    String name,
    String displayName,
    String description,
    String appId,
    String subAppId,
    String accessUrl,
    String publishStatus,
    String iconStorage,
    String currentBundleId,
    Integer onlineInstances,
    Integer totalInstances,
    String bundleVersion,
    String bundleDigest,
    String syncStatus,
    List<PlatformAgentInstance> instances,
    Map<String, Object> metrics,
    List<PlatformLoadedAgent> loadedAgents,
    String spaceId,
    Boolean routingAgentEnabled,
    String routingAgentModelId,
    Integer routingAgentPriority
) {
}
