package com.evalsystem.mock.service.impl;

import com.evalsystem.mock.config.MockAgentProperties;
import com.evalsystem.mock.dto.MockAgentChatRequest;
import com.evalsystem.mock.dto.MockAgentChatResponse;
import com.evalsystem.mock.dto.MockAgentChoice;
import com.evalsystem.mock.dto.MockAgentDelta;
import com.evalsystem.mock.dto.MockAgentDeltaContent;
import com.evalsystem.mock.dto.MockAgentDefinition;
import com.evalsystem.mock.dto.MockAgentField;
import com.evalsystem.mock.dto.MockAgentMessage;
import com.evalsystem.mock.dto.MockAgentVersion;
import com.evalsystem.mock.dto.MockEvaluatorRequest;
import com.evalsystem.mock.dto.MockEvaluatorResponse;
import com.evalsystem.mock.service.MockRuntimeService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MockRuntimeServiceImpl implements MockRuntimeService {
  private static final String STATUS_COMPLETED = "completed";
  private static final String STATUS_FAILED = "failed";
  private static final String ROLE_ASSISTANT = "assistant";
  private static final String ROLE_USER = "user";
  private static final String DEFAULT_AGENT_ALIAS = "router-agent";
  private static final String MOCK_RESPONSE_OBJECT = "com.evalsystem.mock.dto.MockAgentChatResponse";
  private static final String MOCK_MODEL = "mock-super-agent-model";
  private static final BigDecimal DEFAULT_SCORE_MIN = BigDecimal.ONE;
  private static final BigDecimal DEFAULT_SCORE_MAX = BigDecimal.valueOf(5);
  private static final Pattern PROMPT_PARAM_PATTERN = Pattern.compile("\\$\\{([a-zA-Z_][\\w]*)}");
  private static final Pattern FORCE_AGENT_OUTPUT_PATTERN = Pattern.compile("\\[mock:agent_output=([^\\]]*)]");
  private static final Pattern FORCE_AGENT_DEBUG_PATTERN = Pattern.compile("\\[mock:debug=([^\\]]*)]");
  private static final Pattern FORCE_AGENT_REASONING_PATTERN = Pattern.compile("\\[mock:reasoning=([^\\]]*)]");
  private static final Pattern FORCE_SCORE_PATTERN = Pattern.compile("\\[mock:score=([+-]?\\d+(?:\\.\\d+)?)\\]");
  private static final TypeReference<List<Map<String, Object>>> TOOL_CALLS_TYPE = new TypeReference<>() {
  };
  private static final TypeReference<Map<String, Object>> EXTRA_TYPE = new TypeReference<>() {
  };

  private final MockAgentProperties agentProperties;
  private final ObjectMapper objectMapper;

  public MockRuntimeServiceImpl(MockAgentProperties agentProperties, ObjectMapper objectMapper) {
    this.agentProperties = agentProperties;
    this.objectMapper = objectMapper;
  }

  @Override
  public List<MockAgentDefinition> listAgents() {
    return List.of(new MockAgentDefinition(
        DEFAULT_AGENT_ALIAS,
        "Mock超级智能体",
        "用于评测任务联调的本地模拟超级智能体，按debug/reasoning/text三类消息模拟SSE输出。",
        List.of(new MockAgentVersion(DEFAULT_AGENT_ALIAS + "-v1", "V1")),
        List.of(new MockAgentField("query", "query", "string", "用户输入或问题", 1)),
        List.of(
            new MockAgentField("text", "text", "string", "返回给用户的信息", 1),
            new MockAgentField("reasoning", "reasoning", "string", "智能体思考过程", 2),
            new MockAgentField("debug", "debug", "string", "智能体调试信息", 3),
            new MockAgentField("rawText", "rawText", "string", "三类消息合并后的原始文本", 4))));
  }

  @Override
  public MockAgentChatResponse invokeAgent(String agentAlias, MockAgentChatRequest request) {
    long startedAt = System.currentTimeMillis();
    String safeAgentAlias = StringUtils.hasText(agentAlias) ? agentAlias.trim() : DEFAULT_AGENT_ALIAS;
    String conversationId = StringUtils.hasText(request == null ? null : request.conversationId())
        ? request.conversationId().trim()
        : UUID.randomUUID().toString().replace("-", "");
    String latestUserContent = latestUserContent(request == null ? List.of() : request.message());
    if (containsToken(latestUserContent, "[mock:agent_fail]")) {
      return agentFailure(safeAgentAlias, conversationId, startedAt, "Mock智能体按指令返回失败");
    }
    if (containsToken(latestUserContent, "[mock:timeout]")) {
      return agentFailure(safeAgentAlias, conversationId, startedAt, "Mock智能体按指令模拟超时");
    }
    if (StringUtils.hasText(agentProperties.getUrl())) {
      return invokeConfiguredAgent(safeAgentAlias, request, startedAt, conversationId);
    }

    String forcedOutput = firstMatch(FORCE_AGENT_OUTPUT_PATTERN, latestUserContent);
    String text = StringUtils.hasText(forcedOutput)
        ? forcedOutput.trim()
        : "Mock智能体回复：" + truncate(cleanMockTokens(latestUserContent), 500);
    String debug = firstNonBlank(
        firstMatch(FORCE_AGENT_DEBUG_PATTERN, latestUserContent),
        "Mock调试信息：x-agent-alias=" + safeAgentAlias + "，stream=" + Boolean.TRUE.equals(request == null ? null : request.stream()));
    String reasoning = firstNonBlank(
        firstMatch(FORCE_AGENT_REASONING_PATTERN, latestUserContent),
        "Mock思考过程：读取用户输入，抽取关键信息，并生成用于评测的稳定回复。");
    Map<String, String> outputs = new LinkedHashMap<>();
    outputs.put("debug", debug);
    outputs.put("reasoning", reasoning);
    outputs.put("text", text);
    outputs.put("answer", text);
    outputs.put("content", text);
    outputs.put("rawText", joinNonBlank("\n", debug, reasoning, text));
    List<MockAgentChoice> choices = List.of(
        choice(0, contentBlock("debug", debug)),
        choice(1, reasoningBlock(reasoning)),
        choice(2, contentBlock("text", text)));
    return agentResponse(
        safeAgentAlias,
        conversationId,
        STATUS_COMPLETED,
        choices,
        outputs,
        System.currentTimeMillis() - startedAt,
        "",
        outputs.get("rawText"));
  }

  private MockAgentChatResponse invokeConfiguredAgent(
      String agentAlias,
      MockAgentChatRequest request,
      long startedAt,
      String fallbackConversationId
  ) {
    HttpURLConnection connection = null;
    try {
      MockAgentChatRequest outboundRequest = new MockAgentChatRequest(
          fallbackConversationId,
          request == null || request.message() == null ? List.of() : request.message(),
          true);
      connection = (HttpURLConnection) URI.create(agentProperties.getUrl()).toURL().openConnection();
      connection.setRequestMethod("POST");
      connection.setConnectTimeout(Math.max(agentProperties.getConnectTimeoutMs(), 1));
      connection.setReadTimeout(Math.max(agentProperties.getReadTimeoutMs(), 1));
      connection.setDoOutput(true);
      connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
      connection.setRequestProperty("Accept", "text/event-stream, application/json");
      connection.setRequestProperty("x-agent-alias", agentAlias);
      byte[] body = objectMapper.writeValueAsBytes(outboundRequest);
      connection.setFixedLengthStreamingMode(body.length);
      try (var outputStream = connection.getOutputStream()) {
        outputStream.write(body);
      }

      int statusCode = connection.getResponseCode();
      if (statusCode < 200 || statusCode >= 300) {
        String errorBody = readAll(connection.getErrorStream());
        return agentFailure(
            agentAlias,
            fallbackConversationId,
            startedAt,
            "真实智能体调用失败，HTTP " + statusCode + "：" + truncate(errorBody, 500));
      }
      return parseAgentStream(agentAlias, fallbackConversationId, startedAt, connection.getInputStream());
    } catch (Exception e) {
      return agentFailure(
          agentAlias,
          fallbackConversationId,
          startedAt,
          "真实智能体调用失败：" + e.getMessage());
    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
  }

  private MockAgentChatResponse parseAgentStream(
      String agentAlias,
      String fallbackConversationId,
      long startedAt,
      InputStream inputStream
  ) throws IOException {
    RemoteAgentAggregate aggregate = new RemoteAgentAggregate(fallbackConversationId);
    if (inputStream == null) {
      return agentFailure(agentAlias, fallbackConversationId, startedAt, "真实智能体响应为空");
    }
    StringBuilder plainText = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        String payload = normalizeSsePayload(line);
        if (!StringUtils.hasText(payload) || "[DONE]".equals(payload)) {
          continue;
        }
        aggregate.rawPayloads.add(payload);
        if (payload.startsWith("{")) {
          mergeAgentPayload(aggregate, payload);
        } else {
          if (!plainText.isEmpty()) {
            plainText.append('\n');
          }
          plainText.append(payload);
        }
      }
    }

    if (aggregate.choices.isEmpty() && StringUtils.hasText(plainText.toString())) {
      aggregate.choices.add(choice(0, contentBlock("text", plainText.toString())));
    }
    Map<String, String> outputs = buildAgentOutputs(aggregate.choices, aggregate.outputs);
    String rawOutput = StringUtils.hasText(outputs.get("rawText"))
        ? outputs.get("rawText")
        : String.join("\n", aggregate.rawPayloads);
    return new MockAgentChatResponse(
        firstNonBlank(aggregate.id, UUID.randomUUID().toString().replace("-", "")),
        firstNonBlank(aggregate.conversationId, fallbackConversationId),
        firstNonBlank(aggregate.masterAgent, agentAlias),
        firstNonBlank(aggregate.metaAgent, ""),
        firstNonBlank(aggregate.object, MOCK_RESPONSE_OBJECT),
        aggregate.created == null ? System.currentTimeMillis() : aggregate.created,
        firstNonBlank(aggregate.nmodel, ""),
        aggregate.choices,
        STATUS_COMPLETED,
        outputs,
        System.currentTimeMillis() - startedAt,
        "",
        rawOutput);
  }

  private MockAgentChatResponse agentResponse(
      String agentAlias,
      String conversationId,
      String status,
      List<MockAgentChoice> choices,
      Map<String, String> outputs,
      Long latencyMs,
      String errorMessage,
      String rawOutput
  ) {
    return new MockAgentChatResponse(
        UUID.randomUUID().toString().replace("-", ""),
        conversationId,
        agentAlias,
        "mock-meta-agent",
        MOCK_RESPONSE_OBJECT,
        System.currentTimeMillis(),
        MOCK_MODEL,
        choices,
        status,
        outputs,
        latencyMs,
        errorMessage,
        rawOutput);
  }

  @Override
  public MockEvaluatorResponse evaluateEvaluator(MockEvaluatorRequest request) {
    long startedAt = System.currentTimeMillis();
    String payload = evaluatorPayload(request);
    if (containsToken(payload, "[mock:evaluator_fail]")) {
      return evaluatorFailure(startedAt, "Mock评估器按指令返回失败");
    }
    if (containsToken(payload, "[mock:timeout]")) {
      return evaluatorFailure(startedAt, "Mock评估器按指令模拟超时");
    }

    BigDecimal score = forcedScore(payload);
    if (score == null) {
      score = deterministicScore(request, payload);
    }
    score = clampScore(score, scoreMin(request), scoreMax(request));
    String reason = "code".equalsIgnoreCase(request == null ? "" : request.evaluatorType())
        ? "Mock Code评估完成，已根据入参生成稳定分数。"
        : "Mock LLM评估完成，已根据渲染后的Prompt生成稳定分数。";
    String rawOutput = "{\"score\":" + score.stripTrailingZeros().toPlainString()
        + ",\"reason\":\"" + escapeJson(reason) + "\"}";
    return new MockEvaluatorResponse(
        STATUS_COMPLETED,
        score,
        reason,
        "",
        rawOutput,
        System.currentTimeMillis() - startedAt);
  }

  private MockAgentChatResponse agentFailure(String agentAlias, String conversationId, long startedAt, String errorMessage) {
    Map<String, String> outputs = new LinkedHashMap<>();
    outputs.put("debug", errorMessage);
    outputs.put("reasoning", "");
    outputs.put("text", "");
    outputs.put("rawText", errorMessage);
    return agentResponse(
        agentAlias,
        conversationId,
        STATUS_FAILED,
        List.of(choice(0, contentBlock("debug", errorMessage))),
        outputs,
        System.currentTimeMillis() - startedAt,
        errorMessage,
        "");
  }

  private MockEvaluatorResponse evaluatorFailure(long startedAt, String errorMessage) {
    return new MockEvaluatorResponse(
        STATUS_FAILED,
        null,
        "",
        errorMessage,
        "",
        System.currentTimeMillis() - startedAt);
  }

  private String latestUserContent(List<MockAgentMessage> messages) {
    if (messages == null || messages.isEmpty()) {
      return "";
    }
    List<MockAgentMessage> safeMessages = new ArrayList<>(messages);
    for (int i = safeMessages.size() - 1; i >= 0; i--) {
      MockAgentMessage message = safeMessages.get(i);
      if (message != null && ROLE_USER.equalsIgnoreCase(message.role())) {
        return message.content() == null ? "" : message.content();
      }
    }
    MockAgentMessage last = safeMessages.getLast();
    return last == null || last.content() == null ? "" : last.content();
  }

  private String evaluatorPayload(MockEvaluatorRequest request) {
    if (request == null) {
      return "";
    }
    String renderedPrompt = StringUtils.hasText(request.renderedPrompt())
        ? request.renderedPrompt()
        : renderPrompt(request.promptTemplate(), request.params());
    return String.join("\n",
        nullToEmpty(request.evaluatorName()),
        nullToEmpty(request.evaluatorType()),
        renderedPrompt,
        nullToEmpty(request.executeCode()),
        paramsToText(request.params()));
  }

  private String renderPrompt(String promptTemplate, Map<String, Object> params) {
    if (!StringUtils.hasText(promptTemplate)) {
      return "";
    }
    Matcher matcher = PROMPT_PARAM_PATTERN.matcher(promptTemplate);
    StringBuffer result = new StringBuffer();
    while (matcher.find()) {
      Object value = params == null ? null : params.get(matcher.group(1));
      matcher.appendReplacement(result, Matcher.quoteReplacement(value == null ? "" : String.valueOf(value)));
    }
    matcher.appendTail(result);
    return result.toString();
  }

  private String paramsToText(Map<String, Object> params) {
    if (params == null || params.isEmpty()) {
      return "";
    }
    List<String> parts = new ArrayList<>();
    for (Map.Entry<String, Object> entry : params.entrySet()) {
      parts.add(entry.getKey() + "=" + Objects.toString(entry.getValue(), ""));
    }
    return String.join("\n", parts);
  }

  private BigDecimal deterministicScore(MockEvaluatorRequest request, String payload) {
    BigDecimal min = scoreMin(request);
    BigDecimal max = scoreMax(request);
    BigDecimal range = max.subtract(min);
    long hash = Integer.toUnsignedLong(Objects.hash(
        request == null ? "" : request.taskItemId(),
        request == null ? "" : request.taskEvaluatorId(),
        payload));
    BigDecimal ratio = BigDecimal.valueOf(hash % 10_000)
        .divide(BigDecimal.valueOf(9_999), 8, RoundingMode.HALF_UP);
    return min.add(range.multiply(ratio)).setScale(4, RoundingMode.HALF_UP);
  }

  private BigDecimal forcedScore(String payload) {
    Matcher matcher = FORCE_SCORE_PATTERN.matcher(payload == null ? "" : payload);
    if (!matcher.find()) {
      return null;
    }
    return new BigDecimal(matcher.group(1));
  }

  private BigDecimal clampScore(BigDecimal score, BigDecimal min, BigDecimal max) {
    if (score.compareTo(min) < 0) {
      return min.setScale(4, RoundingMode.HALF_UP);
    }
    if (score.compareTo(max) > 0) {
      return max.setScale(4, RoundingMode.HALF_UP);
    }
    return score.setScale(4, RoundingMode.HALF_UP);
  }

  private BigDecimal scoreMin(MockEvaluatorRequest request) {
    return request == null || request.scoreMin() == null ? DEFAULT_SCORE_MIN : request.scoreMin();
  }

  private BigDecimal scoreMax(MockEvaluatorRequest request) {
    return request == null || request.scoreMax() == null ? DEFAULT_SCORE_MAX : request.scoreMax();
  }

  private boolean containsToken(String value, String token) {
    return value != null && value.contains(token);
  }

  private String firstMatch(Pattern pattern, String value) {
    Matcher matcher = pattern.matcher(value == null ? "" : value);
    return matcher.find() ? matcher.group(1) : "";
  }

  private String normalizeSsePayload(String line) {
    if (!StringUtils.hasText(line)) {
      return "";
    }
    String trimmed = line.trim();
    if (trimmed.startsWith(":") || trimmed.startsWith("event:") || trimmed.startsWith("id:") || trimmed.startsWith("retry:")) {
      return "";
    }
    if (trimmed.startsWith("data:")) {
      return trimmed.substring("data:".length()).trim();
    }
    return trimmed;
  }

  private void mergeAgentPayload(RemoteAgentAggregate aggregate, String payload) throws IOException {
    JsonNode root = objectMapper.readTree(payload);
    aggregate.id = firstNonBlank(textValue(root, "id"), aggregate.id);
    aggregate.conversationId = firstNonBlank(textValue(root, "conversationId"), aggregate.conversationId);
    aggregate.masterAgent = firstNonBlank(textValue(root, "masterAgent"), aggregate.masterAgent);
    aggregate.metaAgent = firstNonBlank(textValue(root, "metaAgent"), aggregate.metaAgent);
    aggregate.object = firstNonBlank(textValue(root, "object"), aggregate.object);
    aggregate.nmodel = firstNonBlank(textValue(root, "nmodel"), aggregate.nmodel);
    if (root.hasNonNull("created")) {
      aggregate.created = root.get("created").asLong();
    }
    JsonNode outputsNode = root.get("outputs");
    if (outputsNode != null && outputsNode.isObject()) {
      outputsNode.fields().forEachRemaining(entry -> aggregate.outputs.put(entry.getKey(), scalarText(entry.getValue())));
    }
    JsonNode choicesNode = root.get("choices");
    if (choicesNode != null && choicesNode.isArray()) {
      for (JsonNode choiceNode : choicesNode) {
        aggregate.choices.add(parseChoice(choiceNode, aggregate.choices.size()));
      }
    }
  }

  private MockAgentChoice parseChoice(JsonNode choiceNode, int fallbackIndex) {
    Integer index = choiceNode != null && choiceNode.hasNonNull("index") ? choiceNode.get("index").asInt() : fallbackIndex;
    String finishReason = textValue(choiceNode, "finish_reason");
    JsonNode deltaNode = choiceNode == null ? null : choiceNode.get("delta");
    String role = firstNonBlank(textValue(deltaNode, "role"), ROLE_ASSISTANT);
    List<MockAgentDeltaContent> contents = parseDeltaContents(deltaNode == null ? null : deltaNode.get("content"));
    List<Map<String, Object>> toolCalls = null;
    JsonNode toolCallsNode = deltaNode == null ? null : deltaNode.get("tool_calls");
    if (toolCallsNode != null && !toolCallsNode.isNull()) {
      toolCalls = objectMapper.convertValue(toolCallsNode, TOOL_CALLS_TYPE);
    }
    Map<String, Object> extra = null;
    JsonNode extraNode = deltaNode == null ? null : deltaNode.get("extra");
    if (extraNode != null && !extraNode.isNull()) {
      extra = objectMapper.convertValue(extraNode, EXTRA_TYPE);
    }
    return new MockAgentChoice(index, new MockAgentDelta(role, contents, toolCalls, extra), finishReason);
  }

  private List<MockAgentDeltaContent> parseDeltaContents(JsonNode contentNode) {
    if (contentNode == null || contentNode.isNull()) {
      return List.of();
    }
    List<MockAgentDeltaContent> contents = new ArrayList<>();
    if (contentNode.isArray()) {
      for (JsonNode item : contentNode) {
        contents.add(parseDeltaContent(item));
      }
    } else {
      contents.add(parseDeltaContent(contentNode));
    }
    return contents;
  }

  private MockAgentDeltaContent parseDeltaContent(JsonNode item) {
    if (item == null || item.isNull()) {
      return new MockAgentDeltaContent("text", "", null);
    }
    if (item.isTextual()) {
      return new MockAgentDeltaContent("text", item.asText(), null);
    }
    String type = firstNonBlank(textValue(item, "type"), "text");
    String text = textValue(item, "text");
    String reasoning = textValue(item, "reasoning");
    String fallbackValue = firstNonBlank(text, reasoning, firstNonTypeFieldValue(item));
    if ("reasoning".equals(type)) {
      return new MockAgentDeltaContent(type, null, fallbackValue);
    }
    return new MockAgentDeltaContent(type, fallbackValue, null);
  }

  private String firstNonTypeFieldValue(JsonNode item) {
    if (item == null || !item.isObject()) {
      return "";
    }
    var fields = item.fields();
    while (fields.hasNext()) {
      Map.Entry<String, JsonNode> entry = fields.next();
      if (!"type".equals(entry.getKey())) {
        String value = scalarText(entry.getValue());
        if (StringUtils.hasText(value)) {
          return value;
        }
      }
    }
    return "";
  }

  private Map<String, String> buildAgentOutputs(List<MockAgentChoice> choices, Map<String, String> remoteOutputs) {
    Map<String, String> outputs = new LinkedHashMap<>();
    List<String> debugParts = new ArrayList<>();
    List<String> reasoningParts = new ArrayList<>();
    List<String> textParts = new ArrayList<>();
    for (MockAgentChoice choice : choices) {
      if (choice == null || choice.delta() == null || choice.delta().content() == null) {
        continue;
      }
      for (MockAgentDeltaContent content : choice.delta().content()) {
        appendContentPart(debugParts, reasoningParts, textParts, content);
      }
    }
    putIfPresent(outputs, "debug", joinNonBlank("\n", debugParts.toArray(String[]::new)));
    putIfPresent(outputs, "reasoning", joinNonBlank("\n", reasoningParts.toArray(String[]::new)));
    putIfPresent(outputs, "text", joinNonBlank("\n", textParts.toArray(String[]::new)));
    if (remoteOutputs != null) {
      remoteOutputs.forEach((key, value) -> {
        if (StringUtils.hasText(key) && !outputs.containsKey(key)) {
          outputs.put(key, value == null ? "" : value);
        }
      });
    }
    putIfPresent(outputs, "answer", firstNonBlank(outputs.get("answer"), outputs.get("text"), outputs.get("content")));
    putIfPresent(outputs, "content", firstNonBlank(outputs.get("content"), outputs.get("text"), outputs.get("answer")));
    putIfPresent(outputs, "rawText", firstNonBlank(
        outputs.get("rawText"),
        joinNonBlank("\n", outputs.get("debug"), outputs.get("reasoning"), outputs.get("text"))));
    return outputs;
  }

  private void appendContentPart(
      List<String> debugParts,
      List<String> reasoningParts,
      List<String> textParts,
      MockAgentDeltaContent content
  ) {
    if (content == null || !StringUtils.hasText(content.type())) {
      return;
    }
    String value = firstNonBlank(content.text(), content.reasoning());
    if (!StringUtils.hasText(value)) {
      return;
    }
    if ("debug".equals(content.type())) {
      debugParts.add(value);
    } else if ("reasoning".equals(content.type())) {
      reasoningParts.add(value);
    } else if ("text".equals(content.type())) {
      textParts.add(value);
    }
  }

  private String textValue(JsonNode node, String fieldName) {
    if (node == null || !node.hasNonNull(fieldName)) {
      return "";
    }
    return scalarText(node.get(fieldName));
  }

  private String scalarText(JsonNode node) {
    if (node == null || node.isNull()) {
      return "";
    }
    if (node.isTextual()) {
      return node.asText();
    }
    if (node.isNumber() || node.isBoolean()) {
      return node.asText();
    }
    return node.toString();
  }

  private void putIfPresent(Map<String, String> outputs, String key, String value) {
    if (StringUtils.hasText(value)) {
      outputs.put(key, value);
    }
  }

  private String readAll(InputStream inputStream) throws IOException {
    if (inputStream == null) {
      return "";
    }
    StringBuilder content = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (!content.isEmpty()) {
          content.append('\n');
        }
        content.append(line);
      }
    }
    return content.toString();
  }

  private MockAgentChoice choice(Integer index, MockAgentDeltaContent content) {
    return new MockAgentChoice(
        index,
        new MockAgentDelta(ROLE_ASSISTANT, List.of(content), null, null),
        index != null && index == 2 ? "stop" : null);
  }

  private MockAgentDeltaContent contentBlock(String type, String text) {
    return new MockAgentDeltaContent(type, text, null);
  }

  private MockAgentDeltaContent reasoningBlock(String reasoning) {
    return new MockAgentDeltaContent("reasoning", null, reasoning);
  }

  private String cleanMockTokens(String value) {
    if (!StringUtils.hasText(value)) {
      return "空输入";
    }
    return value
        .replace("[mock:agent_fail]", "")
        .replace("[mock:evaluator_fail]", "")
        .replace("[mock:timeout]", "")
        .replaceAll("\\[mock:score=[^\\]]*]", "")
        .replaceAll("\\[mock:agent_output=[^\\]]*]", "")
        .replaceAll("\\[mock:debug=[^\\]]*]", "")
        .replaceAll("\\[mock:reasoning=[^\\]]*]", "")
        .trim();
  }

  private String escapeJson(String value) {
    return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  private String truncate(String value, int maxLength) {
    if (value == null || value.length() <= maxLength) {
      return value == null ? "" : value;
    }
    return value.substring(0, maxLength);
  }

  private String nullToEmpty(String value) {
    return value == null ? "" : value;
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (StringUtils.hasText(value)) {
        return value.trim();
      }
    }
    return "";
  }

  private String joinNonBlank(String delimiter, String... values) {
    List<String> parts = new ArrayList<>();
    for (String value : values) {
      if (StringUtils.hasText(value)) {
        parts.add(value);
      }
    }
    return String.join(delimiter, parts);
  }

  private static class RemoteAgentAggregate {
    private String id = "";
    private String conversationId;
    private String masterAgent = "";
    private String metaAgent = "";
    private String object = "";
    private Long created;
    private String nmodel = "";
    private final List<MockAgentChoice> choices = new ArrayList<>();
    private final Map<String, String> outputs = new LinkedHashMap<>();
    private final List<String> rawPayloads = new ArrayList<>();

    private RemoteAgentAggregate(String conversationId) {
      this.conversationId = conversationId;
    }
  }
}
