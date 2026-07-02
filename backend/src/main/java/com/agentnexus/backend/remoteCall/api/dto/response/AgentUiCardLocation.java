package com.agentnexus.backend.remoteCall.api.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum AgentUiCardLocation {
  CHAT_UI,
  WEB_UI;

  @JsonCreator
  public static AgentUiCardLocation from(String value) {
    if (value == null) {
      return null;
    }
    for (AgentUiCardLocation location : values()) {
      if (location.name().equalsIgnoreCase(value)) {
        return location;
      }
    }
    return null;
  }
}
