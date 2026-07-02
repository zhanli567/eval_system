package com.agentnexus.backend.remoteCall.api.dto.response;

import java.util.List;

public record AgentBundleListResult(
    String superAgentId,
    String currentBundleId,
    List<AgentBundleItem> items
) {
}
