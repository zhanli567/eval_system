package com.agentnexus.backend.remoteCall.api.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Agent generated UI card location.
 * Created on 2026-08-04.
 */
@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum UICardLocation {
  @JsonPropertyDescription("UI卡片生成到对话框(ChatUI)")
  CHAT_UI,

  @JsonPropertyDescription("UI卡片生成到WebUI")
  WEB_UI
}
