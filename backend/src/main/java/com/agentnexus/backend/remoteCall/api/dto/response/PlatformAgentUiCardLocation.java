package com.agentnexus.backend.remoteCall.api.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum PlatformAgentUiCardLocation {
  CHAT_UI,
  WEB_UI;

  @JsonCreator
  public static PlatformAgentUiCardLocation from(String value) {
    if (value == null) {
      return null;
    }
    for (PlatformAgentUiCardLocation location : values()) {
      if (location.name().equalsIgnoreCase(value)) {
        return location;
      }
    }
    return null;
  }
}
