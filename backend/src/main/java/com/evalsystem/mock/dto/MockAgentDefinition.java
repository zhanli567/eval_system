package com.evalsystem.mock.dto;

import java.util.List;

public record MockAgentDefinition(
    String id,
    String agentName,
    String description,
    List<MockAgentVersion> versions,
    List<MockAgentField> inputs,
    List<MockAgentField> outputs
) {
}
