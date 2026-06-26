package com.agentnexus.backend.integration.api.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PlatformAgentInstance(
    String instanceId,
    String version,
    String sdkVersion,
    String endpoint,
    List<String> capabilities,
    Map<String, Object> systemInfo,
    String status,
    String lastHeartbeatAt,
    String bundleVersion,
    String bundleDigest,
    String syncStatus,
    String lastSyncAt
) {
}
