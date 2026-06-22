package com.evalsystem.integration.api.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PlatformAgentChoice(
    Integer index,
    PlatformAgentDelta delta,
    @JsonProperty("finish_reason") String finishReason
) {
}
