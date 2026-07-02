package com.agentnexus.backend.remoteCall.api.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AgentReferenceSourceType {
  DOCUMENT("document"),
  WEB("web"),
  KNOWLEDGE("knowledge"),
  TICKET("ticket"),
  UNKNOWN("unknown");

  private final String value;

  AgentReferenceSourceType(String value) {
    this.value = value;
  }

  @JsonValue
  public String value() {
    return value;
  }

  @JsonCreator
  public static AgentReferenceSourceType from(String value) {
    if (value == null) {
      return UNKNOWN;
    }
    for (AgentReferenceSourceType sourceType : values()) {
      if (sourceType.value.equalsIgnoreCase(value) || sourceType.name().equalsIgnoreCase(value)) {
        return sourceType;
      }
    }
    return UNKNOWN;
  }
}
