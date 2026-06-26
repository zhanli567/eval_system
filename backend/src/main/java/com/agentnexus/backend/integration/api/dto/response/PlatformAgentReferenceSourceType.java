package com.agentnexus.backend.integration.api.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PlatformAgentReferenceSourceType {
  DOCUMENT("document"),
  WEB("web"),
  KNOWLEDGE("knowledge"),
  TICKET("ticket"),
  UNKNOWN("unknown");

  private final String value;

  PlatformAgentReferenceSourceType(String value) {
    this.value = value;
  }

  @JsonValue
  public String value() {
    return value;
  }

  @JsonCreator
  public static PlatformAgentReferenceSourceType from(String value) {
    if (value == null) {
      return UNKNOWN;
    }
    for (PlatformAgentReferenceSourceType sourceType : values()) {
      if (sourceType.value.equalsIgnoreCase(value) || sourceType.name().equalsIgnoreCase(value)) {
        return sourceType;
      }
    }
    return UNKNOWN;
  }
}
