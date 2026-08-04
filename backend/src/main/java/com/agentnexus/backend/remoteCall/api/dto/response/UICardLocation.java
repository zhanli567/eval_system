package com.agentnexus.backend.remoteCall.api.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Agent generated UI card location.
 * Created on 2026-08-04.
 */
@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum UICardLocation {
  @JsonPropertyDescription("UI card generated in ChatUI")
  CHAT_UI,

  @JsonPropertyDescription("UI card generated in WebUI")
  WEB_UI
}
