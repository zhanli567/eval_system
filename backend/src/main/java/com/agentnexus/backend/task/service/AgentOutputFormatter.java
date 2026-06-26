package com.agentnexus.backend.task.service;

import java.util.Map;
import org.springframework.util.StringUtils;

final class AgentOutputFormatter {
  private AgentOutputFormatter() {
  }

  static String toDisplayText(Map<String, String> outputs) {
    if (outputs == null || outputs.isEmpty()) {
      return "";
    }
    return cleanup(firstNonBlank(
        outputs.get("text"),
        outputs.get("content"),
        outputs.get("answer"),
        outputs.get("error"),
        outputs.get("rawText"),
        outputs.get("reasoning"),
        outputs.get("debug")));
  }

  private static String cleanup(String value) {
    if (!StringUtils.hasText(value)) {
      return "";
    }
    return value
        .replace("\\r\\n", "\n")
        .replace("\\n", "\n")
        .replace("\\r", "\n")
        .replaceAll("\\*\\*([^*]+)\\*\\*", "$1")
        .replaceAll("__([^_]+)__", "$1")
        .replaceAll("[ \\t]+\\n", "\n")
        .replaceAll("\\n{3,}", "\n\n")
        .trim();
  }

  private static String firstNonBlank(String... values) {
    for (String value : values) {
      if (StringUtils.hasText(value)) {
        return value;
      }
    }
    return "";
  }
}
