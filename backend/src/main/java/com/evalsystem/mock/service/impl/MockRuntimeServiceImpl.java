package com.evalsystem.mock.service.impl;

import com.evalsystem.mock.dto.MockAgentChatRequest;
import com.evalsystem.mock.dto.MockAgentChatResponse;
import com.evalsystem.mock.dto.MockAgentDefinition;
import com.evalsystem.mock.dto.MockAgentField;
import com.evalsystem.mock.dto.MockAgentMessage;
import com.evalsystem.mock.dto.MockAgentVersion;
import com.evalsystem.mock.dto.MockEvaluatorRequest;
import com.evalsystem.mock.dto.MockEvaluatorResponse;
import com.evalsystem.mock.service.MockRuntimeService;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
  private static final BigDecimal DEFAULT_SCORE_MIN = BigDecimal.ONE;
  private static final BigDecimal DEFAULT_SCORE_MAX = BigDecimal.valueOf(5);
  private static final Pattern PROMPT_PARAM_PATTERN = Pattern.compile("\\$\\{([a-zA-Z_][\\w]*)}");
  private static final Pattern FORCE_AGENT_OUTPUT_PATTERN = Pattern.compile("\\[mock:agent_output=([^\\]]*)]");
  private static final Pattern FORCE_SCORE_PATTERN = Pattern.compile("\\[mock:score=([+-]?\\d+(?:\\.\\d+)?)\\]");

  @Override
  public List<MockAgentDefinition> listAgents() {
    return List.of(new MockAgentDefinition(
        "mock-agent",
        "Mock智能体",
        "用于评测任务联调的本地模拟智能体，会把映射后的输入拼接为回答。",
        List.of(new MockAgentVersion("mock-agent-v1", "V1")),
        List.of(new MockAgentField("query", "query", "string", "用户输入或问题", 1)),
        List.of(
            new MockAgentField("answer", "answer", "string", "标准应用输出字段", 1),
            new MockAgentField("content", "content", "string", "兼容消息内容字段", 2),
            new MockAgentField("rawText", "rawText", "string", "原始文本输出字段", 3))));
  }

  @Override
  public MockAgentChatResponse invokeAgent(MockAgentChatRequest request) {
    long startedAt = System.currentTimeMillis();
    String conversationId = StringUtils.hasText(request == null ? null : request.conversationId())
        ? request.conversationId().trim()
        : UUID.randomUUID().toString().replace("-", "");
    String latestUserContent = latestUserContent(request == null ? List.of() : request.message());
    if (containsToken(latestUserContent, "[mock:agent_fail]")) {
      return agentFailure(conversationId, startedAt, "Mock智能体按指令返回失败");
    }
    if (containsToken(latestUserContent, "[mock:timeout]")) {
      return agentFailure(conversationId, startedAt, "Mock智能体按指令模拟超时");
    }

    String forcedOutput = firstMatch(FORCE_AGENT_OUTPUT_PATTERN, latestUserContent);
    String content = StringUtils.hasText(forcedOutput)
        ? forcedOutput.trim()
        : "Mock智能体回复：" + truncate(cleanMockTokens(latestUserContent), 500);
    Map<String, String> outputs = new LinkedHashMap<>();
    outputs.put("answer", content);
    outputs.put("content", content);
    outputs.put("rawText", content);
    return new MockAgentChatResponse(
        conversationId,
        new MockAgentMessage(content, ROLE_ASSISTANT),
        STATUS_COMPLETED,
        outputs,
        System.currentTimeMillis() - startedAt,
        "",
        content);
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

  private MockAgentChatResponse agentFailure(String conversationId, long startedAt, String errorMessage) {
    return new MockAgentChatResponse(
        conversationId,
        new MockAgentMessage("", ROLE_ASSISTANT),
        STATUS_FAILED,
        Map.of(),
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
}
