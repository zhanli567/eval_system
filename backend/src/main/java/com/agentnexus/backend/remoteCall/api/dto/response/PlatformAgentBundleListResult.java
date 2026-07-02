package com.agentnexus.backend.remoteCall.api.dto.response;

import java.util.List;

public record PlatformAgentBundleListResult(
    String superAgentId,
    String currentBundleId,
    List<PlatformAgentBundleItem> items
) {
}
