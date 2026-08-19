package com.agentnexus.backend.remoteCall.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.agentnexus.backend.remoteCall.api.dto.response.ChatCompletionChunk;
import com.agentnexus.backend.remoteCall.api.dto.response.ChatCompletionChunk.DeltaContent;
import com.agentnexus.backend.remoteCall.config.RemoteCallProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Method;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RemoteCallServiceTrajectoryContentTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private Method parseDeltaContent;

  @BeforeEach
  void setUp() throws NoSuchMethodException {
    parseDeltaContent = RemoteCallService.class.getDeclaredMethod("parseDeltaContent", JsonNode.class);
    parseDeltaContent.setAccessible(true);
  }

  @Test
  void parsesTrajectoryContentAndProtectsPayloadFromMutation() throws Exception {
    DeltaContent content = parse("""
        {
          "type": "trajectory",
          "sequence": 7,
          "eventType": "tool_call",
          "stage": "planning",
          "timestamp": 1710000000123,
          "eventId": "event-7",
          "parentId": "event-2",
          "payload": {"tool": "search", "attempt": 1}
        }
        """);

    ChatCompletionChunk.TrajectoryContent trajectory =
        assertInstanceOf(ChatCompletionChunk.TrajectoryContent.class, content);
    assertEquals(7, trajectory.getSequence());
    assertEquals("tool_call", trajectory.getEventType());
    assertEquals("planning", trajectory.getStage());
    assertEquals(1710000000123L, trajectory.getTimestamp());
    assertEquals("event-7", trajectory.getEventId());
    assertEquals("event-2", trajectory.getParentId());
    assertEquals(Map.of("tool", "search", "attempt", 1), trajectory.getPayload());
    assertThrows(
        UnsupportedOperationException.class,
        () -> trajectory.getPayload().put("new-key", "new-value"));
  }

  @Test
  void parsesExecutionMetricsContentWithNullableTokenFieldsAndImmutableAttributes() throws Exception {
    DeltaContent content = parse("""
        {
          "type": "execution_metrics",
          "latencyMs": 42,
          "modelCallCount": 2,
          "toolCallCount": 3,
          "inputTokens": null,
          "outputTokens": 128,
          "attributes": {"region": "cn", "cached": true}
        }
        """);

    ChatCompletionChunk.ExecutionMetricsContent metrics =
        assertInstanceOf(ChatCompletionChunk.ExecutionMetricsContent.class, content);
    assertEquals(42L, metrics.getLatencyMs());
    assertEquals(2, metrics.getModelCallCount());
    assertEquals(3, metrics.getToolCallCount());
    assertNull(metrics.getInputTokens());
    assertEquals(128L, metrics.getOutputTokens());
    assertEquals(Map.of("region", "cn", "cached", true), metrics.getAttributes());
    assertThrows(
        UnsupportedOperationException.class,
        () -> metrics.getAttributes().put("new-key", "new-value"));
  }

  private DeltaContent parse(String json) throws Exception {
    RemoteCallService service =
        new RemoteCallService(new RemoteCallProperties(), objectMapper, null, null);
    return (DeltaContent) parseDeltaContent.invoke(service, objectMapper.readTree(json));
  }
}
