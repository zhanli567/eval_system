package com.agentnexus.backend.remoteCall.api.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AgentChoice(
    Integer index,
    AgentDelta delta,
    @JsonProperty("finish_reason") String finishReason
) {
}
