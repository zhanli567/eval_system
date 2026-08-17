package com.agentnexus.backend.remoteCall.api.dto.response;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Agent streaming chat completion chunk.
 * Created on 2026-08-04.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatCompletionChunk {
  private String id;
  private String conversationId;
  private String masterAgent;
  private String metaAgent;
  private String userId;
  private String object;
  private Long created;
  private String model;
  private List<Choice> choices;

  /**
   * Agent streaming choice item.
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Choice {
    private Integer index;
    private Delta delta;
    private String finish_reason;
  }

  /**
   * Agent streaming delta body.
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Delta {
    private String role;
    private List<? extends DeltaContent> content;
    private List<ToolCallDelta> tool_calls;
    private Map<String, Object> extra;
  }

  /**
   * Base type for a typed agent content block.
   */
  @Getter
  public static class DeltaContent {
    private final String type;

    /**
     * Creates a typed agent content block.
     *
     * @param type content block type
     */
    protected DeltaContent(String type) {
      this.type = type;
    }
  }

  /**
   * Reasoning content block.
   */
  public static class ReasoningContent extends DeltaContent {
    @Getter
    @Setter
    private String reasoning;

    /**
     * Creates a reasoning content block.
     *
     * @param reasoning reasoning text
     */
    public ReasoningContent(String reasoning) {
      super("reasoning");
      this.reasoning = reasoning;
    }
  }

  /**
   * Skill trigger content block.
   */
  @Getter
  public static class SkillTriggerContent extends DeltaContent {
    private final String skillName;
    private final String skillDesc;

    /**
     * Creates a skill trigger content block.
     *
     * @param skillName skill name
     * @param skillDesc skill description
     */
    public SkillTriggerContent(String skillName, String skillDesc) {
      super("skill_trigger");
      this.skillName = skillName;
      this.skillDesc = skillDesc;
    }
  }

  /**
   * References content block.
   */
  @Getter
  public static class ReferencesContent extends DeltaContent {
    private final List<ReferenceItem> references;

    /**
     * Creates a references content block.
     *
     * @param references reference items
     */
    public ReferencesContent(List<ReferenceItem> references) {
      super("references");
      this.references = Collections.unmodifiableList(references);
    }
  }

  /**
   * Error content block.
   */
  @Getter
  public static class ErrorContent extends DeltaContent {
    private final String error;

    /**
     * Creates an error content block.
     *
     * @param error error text
     */
    public ErrorContent(String error) {
      super("error");
      this.error = error;
    }
  }

  /**
   * Debug content block.
   */
  @Getter
  @Setter
  public static class DebugContent extends DeltaContent {
    private String text;

    /**
     * Creates a debug content block.
     *
     * @param text debug text
     */
    public DebugContent(String text) {
      super("debug");
      this.text = text;
    }
  }

  /**
   * Text content block.
   */
  @Getter
  @Setter
  public static class TextContent extends DeltaContent {
    private String text;

    /**
     * Creates a text content block.
     *
     * @param text text content
     */
    public TextContent(String text) {
      super("text");
      this.text = text;
    }
  }

  /**
   * Tool call content block.
   */
  @Getter
  public static class ToolCallContent extends DeltaContent {
    private final String toolCallId;
    private final String toolName;
    private final String arguments;

    /**
     * Creates a tool call content block.
     *
     * @param toolCallId tool call id
     * @param toolName tool name
     * @param arguments tool call arguments
     */
    public ToolCallContent(String toolCallId, String toolName, String arguments) {
      super("tool_call");
      this.toolCallId = toolCallId;
      this.toolName = toolName;
      this.arguments = arguments;
    }
  }

  /**
   * Tool response content block.
   */
  @Getter
  public static class ToolResponseContent extends DeltaContent {
    private final String toolCallId;
    private final String toolName;
    private final String response;

    /**
     * Creates a tool response content block.
     *
     * @param toolCallId tool call id
     * @param toolName tool name
     * @param response tool response content
     */
    public ToolResponseContent(String toolCallId, String toolName, String response) {
      super("tool_response");
      this.toolCallId = toolCallId;
      this.toolName = toolName;
      this.response = response;
    }
  }

  /**
   * Generated UI content block.
   */
  @Getter
  public static class GenUIContent extends DeltaContent {
    private final UICardDefinition uiCardDefinition;

    /**
     * Creates a generated UI content block.
     *
     * @param uiCardDefinition generated UI card definition
     */
    public GenUIContent(UICardDefinition uiCardDefinition) {
      super("gen_ui");
      this.uiCardDefinition = uiCardDefinition;
    }
  }

  /**
   * Agent trajectory content block.
   */
  @Getter
  public static class TrajectoryContent extends DeltaContent {
    private final int sequence;
    private final String eventType;
    private final String stage;
    private final long timestamp;
    private final String eventId;
    private final String parentId;
    private final Map<String, Object> payload;

    /**
     * Creates a trajectory content block.
     *
     * @param sequence event sequence
     * @param eventType event type
     * @param stage event stage
     * @param timestamp event timestamp
     * @param eventId event id
     * @param parentId parent event id
     * @param payload event payload
     */
    public TrajectoryContent(
        int sequence,
        String eventType,
        String stage,
        long timestamp,
        String eventId,
        String parentId,
        Map<String, Object> payload
    ) {
      super("trajectory");
      this.sequence = sequence;
      this.eventType = eventType;
      this.stage = stage;
      this.timestamp = timestamp;
      this.eventId = eventId;
      this.parentId = parentId;
      this.payload = payload == null ? Map.of() : Map.copyOf(payload);
    }
  }

  /**
   * Execution metrics content block.
   */
  @Getter
  public static class ExecutionMetricsContent extends DeltaContent {
    private final long latencyMs;
    private final int modelCallCount;
    private final int toolCallCount;
    private final Long inputTokens;
    private final Long outputTokens;
    private final Map<String, Object> attributes;

    /**
     * Creates an execution metrics content block.
     *
     * @param latencyMs execution latency in milliseconds
     * @param modelCallCount model call count
     * @param toolCallCount tool call count
     * @param inputTokens input token count
     * @param outputTokens output token count
     * @param attributes additional metrics attributes
     */
    public ExecutionMetricsContent(
        long latencyMs,
        int modelCallCount,
        int toolCallCount,
        Long inputTokens,
        Long outputTokens,
        Map<String, Object> attributes
    ) {
      super("execution_metrics");
      this.latencyMs = latencyMs;
      this.modelCallCount = modelCallCount;
      this.toolCallCount = toolCallCount;
      this.inputTokens = inputTokens;
      this.outputTokens = outputTokens;
      this.attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
  }

  /**
   * Streaming tool call delta.
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ToolCallDelta {
    private Integer index;
    private String id;
    private String type;
    private FunctionDelta function;
  }

  /**
   * Streaming tool function delta.
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class FunctionDelta {
    private String name;
    private String arguments;
  }
}
