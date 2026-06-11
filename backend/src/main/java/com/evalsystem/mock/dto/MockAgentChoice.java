package com.evalsystem.mock.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MockAgentChoice(
    Integer index,
    MockAgentDelta delta,
    @JsonProperty("finish_reason") String finishReason
) {
}
