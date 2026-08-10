package com.agentnexus.backend.task.service;

import com.agentnexus.backend.remoteCall.constant.AgentOutputFieldConstants;
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
        outputs.get(AgentOutputFieldConstants.TEXT),
        outputs.get(AgentOutputFieldConstants.CONTENT),
        outputs.get(AgentOutputFieldConstants.ANSWER),
        outputs.get(AgentOutputFieldConstants.ERROR),
        outputs.get(AgentOutputFieldConstants.RAW_TEXT),
        outputs.get(AgentOutputFieldConstants.REASONING),
        outputs.get(AgentOutputFieldConstants.DEBUG),
        outputs.get(AgentOutputFieldConstants.TOOL_CALL),
        outputs.get(AgentOutputFieldConstants.TOOL_RESPONSE),
        outputs.get(AgentOutputFieldConstants.SKILL_TRIGGER),
        outputs.get(AgentOutputFieldConstants.REFERENCES),
        outputs.get(AgentOutputFieldConstants.GEN_UI)));
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
