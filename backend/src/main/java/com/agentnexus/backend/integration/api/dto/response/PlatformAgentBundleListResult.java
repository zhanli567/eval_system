package com.agentnexus.backend.integration.api.dto.response;

import java.util.List;

public record PlatformAgentBundleListResult(
    String superAgentId,
    String currentBundleId,
    List<PlatformAgentBundleItem> items
) {
}
