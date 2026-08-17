package com.agentnexus.backend.remoteCall.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.agentnexus.backend.remoteCall.api.dto.response.ChatCompletionChunk;
import com.agentnexus.backend.remoteCall.api.dto.response.ChatCompletionChunk.Choice;
import com.agentnexus.backend.remoteCall.api.dto.response.ChatCompletionChunk.Delta;
import com.agentnexus.backend.remoteCall.api.dto.response.ChatCompletionChunk.DeltaContent;
import com.agentnexus.backend.remoteCall.api.dto.response.ChatCompletionChunk.ExecutionMetricsContent;
import com.agentnexus.backend.remoteCall.api.dto.response.ChatCompletionChunk.TextContent;
import com.agentnexus.backend.remoteCall.api.dto.response.ChatCompletionChunk.TrajectoryContent;
import com.agentnexus.backend.remoteCall.api.dto.response.AgentChatResponse;
import com.agentnexus.backend.remoteCall.config.RemoteCallProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RemoteCallServiceTrajectoryContentTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private Method parseDeltaContent;
  private Method buildAgentOutputs;
  private Method parseAgentStream;

  @BeforeEach
  void setUp() throws NoSuchMethodException {
    parseDeltaContent = RemoteCallService.class.getDeclaredMethod("parseDeltaContent", JsonNode.class);
    parseDeltaContent.setAccessible(true);
    buildAgentOutputs = RemoteCallService.class.getDeclaredMethod("buildAgentOutputs", List.class);
    buildAgentOutputs.setAccessible(true);
    parseAgentStream = RemoteCallService.class.getDeclaredMethod(
        "parseAgentStream",
        String.class,
        String.class,
        long.class,
        java.io.InputStream.class);
    parseAgentStream.setAccessible(true);
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

  @Test
  void buildsLegacyOutputsAndStructuredSseJsonTogether() throws Exception {
    TrajectoryContent trajectory = new TrajectoryContent(
        1,
        "agent_start",
        "agent",
        1710000000000L,
        "event-1",
        "",
        Map.of("input", "hello"));
    ExecutionMetricsContent metrics = new ExecutionMetricsContent(
        42L,
        1,
        0,
        10L,
        2L,
        Map.of("final", true));
    Choice choice = new Choice(
        0,
        new Delta(
            "assistant",
            List.of(new TextContent("answer"), trajectory, metrics),
            null,
            null),
        "stop");

    @SuppressWarnings("unchecked")
    Map<String, String> outputs = (Map<String, String>) buildAgentOutputs.invoke(
        new RemoteCallService(new RemoteCallProperties(), objectMapper, null, null),
        List.of(choice));

    assertEquals("answer", outputs.get("text"));
    assertEquals("answer", outputs.get("answer"));
    assertEquals(1, objectMapper.readTree(outputs.get("trajectory")).size());
    assertEquals("agent_start", objectMapper.readTree(outputs.get("trajectory")).get(0).get("eventType").asText());
    assertEquals(42L, objectMapper.readTree(outputs.get("executionMetrics")).get("latencyMs").asLong());
  }

  @Test
  void carriesStructuredSseDataInAggregatedAgentResponse() throws Exception {
    String sse = """
        data: {"choices":[{"delta":{"content":[{"type":"text","text":"2"},{"type":"trajectory","sequence":1,"eventType":"agent_finish","stage":"final","payload":{"ok":true}},{"type":"execution_metrics","latencyMs":42,"inputTokens":null,"outputTokens":2,"attributes":{"final":true}}]}}]}
        data: [DONE]
        """;

    AgentChatResponse response = (AgentChatResponse) parseAgentStream.invoke(
        new RemoteCallService(new RemoteCallProperties(), objectMapper, null, null),
        "agent",
        "conversation",
        0L,
        new ByteArrayInputStream(sse.getBytes(StandardCharsets.UTF_8)));

    assertEquals("2", response.outputs().get("answer"));
    assertEquals(1, response.trajectory().size());
    assertEquals("agent_finish", response.trajectory().get(0).get("eventType"));
    assertEquals(42, response.metrics().get("latencyMs"));
    assertNull(response.metrics().get("inputTokens"));
  }

  private DeltaContent parse(String json) throws Exception {
    RemoteCallService service =
        new RemoteCallService(new RemoteCallProperties(), objectMapper, null, null);
    return (DeltaContent) parseDeltaContent.invoke(service, objectMapper.readTree(json));
  }
}
