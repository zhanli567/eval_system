package com.agentnexus.backend.integration.api.dto.response;

public record PlatformAgentBundleItem(
    String bundleId,
    String status,
    String bundleVersion,
    String bundleDigest,
    String publishedAt
) {
}
