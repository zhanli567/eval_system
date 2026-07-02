package com.agentnexus.backend.remoteCall.api.dto.response;

import java.util.List;
import java.util.Map;

public record AgentContentBlock(
    String type,
    String text,
    String reasoning,
    String error,
    String skillName,
    String skillDesc,
    List<AgentReferenceItem> references,
    String toolCallId,
    String toolName,
    String arguments,
    String response,
    AgentUiCardDefinition uiCardDefinition,
    Map<String, Object> extra
) {
  public AgentContentBlock(String type, String text, String reasoning, String error) {
    this(type, text, reasoning, error, null, null, null, null, null, null, null, null, null);
  }
}
