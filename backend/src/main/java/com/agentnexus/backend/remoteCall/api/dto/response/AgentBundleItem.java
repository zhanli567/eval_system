package com.agentnexus.backend.remoteCall.api.dto.response;

public record AgentBundleItem(
    String bundleId,
    String status,
    String bundleVersion,
    String bundleDigest,
    String publishedAt
) {
}
