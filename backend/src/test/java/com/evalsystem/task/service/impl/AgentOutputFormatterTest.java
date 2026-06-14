package com.evalsystem.task.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class AgentOutputFormatterTest {
  @Test
  void prefersCleanTextForDisplay() {
    String output = AgentOutputFormatter.toDisplayText(Map.of(
        "text", "**答案**\\n第一行\\n\\n\\n第二行",
        "rawText", "{\"text\":\"bad\"}"));

    assertThat(output).isEqualTo("答案\n第一行\n\n第二行");
  }

  @Test
  void fallsBackToRawOutputWhenTextIsAbsent() {
    String output = AgentOutputFormatter.toDisplayText(Map.of("rawText", "调试\\n结果"));

    assertThat(output).isEqualTo("调试\n结果");
  }
}
