package com.agentnexus.backend.remoteCall.service;

import com.agentnexus.backend.common.context.CurrentSpaceHolder;
import com.agentnexus.backend.common.context.TaskCookieHolder;
import com.agentnexus.backend.dataset.constant.DatasetFieldTypeConstants;
import com.agentnexus.backend.iam.IamTokenService;
import com.agentnexus.backend.remoteCall.config.RemoteCallProperties;
import com.agentnexus.backend.remoteCall.api.dto.request.AgentChatRequest;
import com.agentnexus.backend.remoteCall.api.dto.response.AgentChild;
import com.agentnexus.backend.remoteCall.api.dto.response.AgentBundleItem;
import com.agentnexus.backend.remoteCall.api.dto.response.AgentBundleListResult;
import com.agentnexus.backend.remoteCall.api.dto.response.AgentChatResponse;
import com.agentnexus.backend.remoteCall.api.dto.response.AgentDefinition;
import com.agentnexus.backend.remoteCall.api.dto.response.AgentField;
import com.agentnexus.backend.remoteCall.api.dto.request.AgentMessage;
import com.agentnexus.backend.remoteCall.api.dto.response.AgentVersion;
import com.agentnexus.backend.remoteCall.api.dto.response.ChatCompletionChunk.Choice;
import com.agentnexus.backend.remoteCall.api.dto.response.ChatCompletionChunk.DebugContent;
import com.agentnexus.backend.remoteCall.api.dto.response.ChatCompletionChunk.Delta;
import com.agentnexus.backend.remoteCall.api.dto.response.ChatCompletionChunk.DeltaContent;
import com.agentnexus.backend.remoteCall.api.dto.response.ChatCompletionChunk.ErrorContent;
import com.agentnexus.backend.remoteCall.api.dto.response.ChatCompletionChunk.GenUIContent;
import com.agentnexus.backend.remoteCall.api.dto.response.ChatCompletionChunk.ReasoningContent;
import com.agentnexus.backend.remoteCall.api.dto.response.ChatCompletionChunk.ReferencesContent;
import com.agentnexus.backend.remoteCall.api.dto.response.ChatCompletionChunk.SkillTriggerContent;
import com.agentnexus.backend.remoteCall.api.dto.response.ChatCompletionChunk.TextContent;
import com.agentnexus.backend.remoteCall.api.dto.response.ChatCompletionChunk.ToolCallContent;
import com.agentnexus.backend.remoteCall.api.dto.response.ChatCompletionChunk.ToolCallDelta;
import com.agentnexus.backend.remoteCall.api.dto.response.ChatCompletionChunk.ToolResponseContent;
import com.agentnexus.backend.remoteCall.api.dto.response.LoadedAgent;
import com.agentnexus.backend.remoteCall.api.dto.response.ListResult;
import com.agentnexus.backend.remoteCall.api.dto.response.ModelChatResult;
import com.agentnexus.backend.remoteCall.api.dto.response.ModelInfo;
import com.agentnexus.backend.remoteCall.api.dto.response.ReferenceItem;
import com.agentnexus.backend.remoteCall.api.dto.response.RemoteResponse;
import com.agentnexus.backend.remoteCall.api.dto.response.SpaceInfo;
import com.agentnexus.backend.remoteCall.api.dto.response.SuperAgentDetail;
import com.agentnexus.backend.remoteCall.api.dto.response.SuperAgentInfo;
import com.agentnexus.backend.remoteCall.api.dto.response.UICardDefinition;
import com.agentnexus.backend.remoteCall.client.RemoteCallServiceClient;
import com.agentnexus.backend.remoteCall.constant.AgentFieldDisplayConstants;
import com.agentnexus.backend.remoteCall.constant.AgentInputFieldConstants;
import com.agentnexus.backend.remoteCall.constant.AgentOutputFieldConstants;
import com.agentnexus.backend.remoteCall.constant.ChatContentTypeConstants;
import com.agentnexus.backend.remoteCall.constant.ChatRoleConstants;
import com.agentnexus.backend.remoteCall.constant.RemoteCallDefaults;
import com.agentnexus.backend.remoteCall.constant.RemoteCallHeaderConstants;
import com.agentnexus.backend.remoteCall.constant.RemoteCallJsonFieldConstants;
import com.agentnexus.backend.remoteCall.constant.RemoteCallMediaTypeConstants;
import com.agentnexus.backend.remoteCall.constant.RemoteCallProtocolConstants;
import com.agentnexus.backend.remoteCall.constant.RemoteCallStatusConstants;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RemoteCallService {
  private static final TypeReference<List<ToolCallDelta>> TOOL_CALLS_TYPE = new TypeReference<>() {
  };
  private static final TypeReference<List<ReferenceItem>> REFERENCES_TYPE = new TypeReference<>() {
  };
  private static final TypeReference<Map<String, Object>> EXTRA_TYPE = new TypeReference<>() {
  };

  private final RemoteCallProperties properties;
  private final ObjectMapper objectMapper;
  private final RemoteCallServiceClient remoteCallServiceClient;
  private final IamTokenService iamTokenService;

  @Autowired
  public RemoteCallService(
      RemoteCallProperties properties,
      ObjectMapper objectMapper,
      RemoteCallServiceClient remoteCallServiceClient,
      IamTokenService iamTokenService
  ) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.remoteCallServiceClient = remoteCallServiceClient;
    this.iamTokenService = iamTokenService;
  }

  public List<ModelInfo> listModels() {
    RemoteResponse<ListResult<ModelInfo>> response = remoteCallServiceClient.listModels(
        RemoteCallDefaults.PAGE_SIZE,
        RemoteCallDefaults.CUR_PAGE,
        CurrentSpaceHolder.get());
    ensureSuccess("模型列表接口", response.status(), response.success());
    ListResult<ModelInfo> result = response.resultObjVO();
    List<ModelInfo> models = result == null || result.result() == null ? List.of() : result.result();
    return models.stream()
        .filter(model -> RemoteCallDefaults.AUTH_TYPE_IAM.equalsIgnoreCase(model.authType()))
        .toList();
  }

  public List<AgentDefinition> listAgents() {
    RemoteResponse<ListResult<SuperAgentInfo>> response = remoteCallServiceClient.listAgents(
        RemoteCallDefaults.PAGE_SIZE,
        RemoteCallDefaults.CUR_PAGE,
        CurrentSpaceHolder.get());
    ensureSuccess("智能体列表接口", response.status(), response.success());
    ListResult<SuperAgentInfo> result = response.resultObjVO();
    if (result == null || result.result() == null) {
      return List.of();
    }
    return result.result().stream().map(this::toAgentDefinition).toList();
  }

  public AgentDefinition getAgentDetail(String agentId) {
    return toAgentDefinition(loadAgentDetail(agentId));
  }

  public List<AgentVersion> listAgentBundles(String agentId) {
    String safeAgentId = requireText(agentId, "Agent ID cannot be blank");
    RemoteResponse<AgentBundleListResult> response = remoteCallServiceClient.listAgentBundles(
        safeAgentId,
        CurrentSpaceHolder.get());
    ensureSuccess("Agent bundle list API", response.status(), response.success());
    return normalizeAgentBundles(response.resultObjVO());
  }

  public List<SpaceInfo> listSpaces(int pageSize, int curPage, String cookie) {
    RemoteResponse<ListResult<SpaceInfo>> response = remoteCallServiceClient.listSpaces(
        pageSize,
        curPage,
        firstNonBlank(cookie));
    ensureSuccess("Space list API", response.status(), response.success());
    ListResult<SpaceInfo> result = response.resultObjVO();
    if (result == null || result.result() == null) {
      return List.of();
    }
    return result.result().stream()
        .filter(space -> RemoteCallStatusConstants.ACTIVE.equalsIgnoreCase(space.status()))
        .toList();
  }

  private SuperAgentDetail loadAgentDetail(String agentId) {
    String safeAgentId = requireText(agentId, "Agent ID cannot be blank");
    RemoteResponse<SuperAgentDetail> response = remoteCallServiceClient.getAgentDetail(safeAgentId, CurrentSpaceHolder.get());
    ensureSuccess("Agent detail API", response.status(), response.success());
    if (response.resultObjVO() == null) {
      throw new IllegalStateException("Agent detail API returned empty result");
    }
    return response.resultObjVO();
  }

  public ModelChatResult chatModel(String modelId, String modelName, String message) {
    return chatIamModel(modelId, modelName, message);
  }

  private ModelChatResult chatIamModel(String modelId, String modelName, String message) {
    requireText(modelId, "模型ID不能为空");
    String safeModelName = requireText(modelName, "模型名称不能为空");
    requireText(properties.getIam().getUrl(), "请配置IAM模型对话接口 remoteCall.iam.url");
    String token = requireText(iamTokenService.getToken(), "IAM token不能为空");
    HttpURLConnection connection = null;
    try {
      connection = openConnection(properties.getIam().getUrl(), RemoteCallProtocolConstants.POST);
      connection.setRequestProperty(RemoteCallHeaderConstants.ACCEPT, RemoteCallMediaTypeConstants.APPLICATION_JSON);
      connection.setRequestProperty(RemoteCallHeaderConstants.CONTENT_TYPE, RemoteCallMediaTypeConstants.APPLICATION_JSON_UTF8);
      connection.setRequestProperty(RemoteCallHeaderConstants.AUTHORIZATION, token);
      Map<String, Object> body = new LinkedHashMap<>();
      body.put(RemoteCallJsonFieldConstants.MODEL, safeModelName);
      body.put(RemoteCallJsonFieldConstants.MESSAGES, List.of(Map.of(
          RemoteCallJsonFieldConstants.ROLE, ChatRoleConstants.USER,
          RemoteCallJsonFieldConstants.CONTENT, message == null ? "" : message)));
      body.put(RemoteCallJsonFieldConstants.STREAM, Boolean.FALSE);
      writeJson(connection, body);
      int statusCode = connection.getResponseCode();
      String responseBody = readAll(statusCode >= 200 && statusCode < 300 ? connection.getInputStream() : connection.getErrorStream());
      if (statusCode < 200 || statusCode >= 300) {
        throw new IllegalStateException("IAM模型对话接口调用失败，HTTP " + statusCode + "：" + truncate(responseBody, 500));
      }
      String outputText = parseIamModelOutput(responseBody);
      return new ModelChatResult(modelId, outputText, String.valueOf(System.currentTimeMillis()));
    } catch (IOException e) {
      throw new IllegalStateException("IAM模型对话接口调用失败：" + e.getMessage(), e);
    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
  }

  private String parseIamModelOutput(String responseBody) throws IOException {
    JsonNode root = objectMapper.readTree(responseBody);
    JsonNode choicesNode = root.get(RemoteCallJsonFieldConstants.CHOICES);
    if (choicesNode == null || !choicesNode.isArray() || choicesNode.isEmpty()) {
      throw new IllegalStateException("IAM模型对话接口返回缺少choices");
    }
    JsonNode messageNode = choicesNode.get(0).get(RemoteCallJsonFieldConstants.MESSAGE);
    String content = textValue(messageNode, RemoteCallJsonFieldConstants.CONTENT);
    if (!StringUtils.hasText(content)) {
      throw new IllegalStateException("IAM模型对话接口返回缺少message.content");
    }
    return cleanThinkContent(content);
  }

  private String cleanThinkContent(String content) {
    String safeContent = content == null ? "" : content;
    int thinkEnd = safeContent.indexOf(RemoteCallProtocolConstants.THINK_END_TAG);
    if (thinkEnd >= 0) {
      return safeContent.substring(thinkEnd + RemoteCallProtocolConstants.THINK_END_TAG.length()).trim();
    }
    return safeContent.trim();
  }

  public AgentChatResponse invokeAgent(
      String agentId,
      String bundleId,
      String agentAlias,
      AgentChatRequest request
  ) {
    String safeAgentId = firstNonBlank(agentId, RemoteCallDefaults.AGENT_ALIAS);
    String safeBundleId = requireText(bundleId, "Agent bundle ID cannot be blank");
    String safeAgentAlias = firstNonBlank(agentAlias, safeAgentId);
    long startedAt = System.currentTimeMillis();
    String conversationId = StringUtils.hasText(request == null ? null : request.conversationId())
        ? request.conversationId()
        : UUID.randomUUID().toString().replace("-", "");
    AgentChatRequest outboundRequest = new AgentChatRequest(
        conversationId,
        request == null || request.messages() == null ? List.of() : request.messages(),
        Boolean.TRUE);
    String chatUrl = requireText(properties.getAgentChatUrl(), "Please configure agent chat API remoteCall.agent-chat-url");

    HttpURLConnection connection = null;
    try {
      connection = openConnection(chatUrl, RemoteCallProtocolConstants.POST);
      connection.setRequestProperty(RemoteCallHeaderConstants.CONTENT_TYPE, RemoteCallMediaTypeConstants.APPLICATION_JSON_UTF8);
      connection.setRequestProperty(RemoteCallHeaderConstants.ACCEPT, RemoteCallMediaTypeConstants.EVENT_STREAM_OR_JSON);
      chatCompletionHeaders().forEach(connection::setRequestProperty);
      connection.setRequestProperty(RemoteCallHeaderConstants.SUPER_AGENT_ID, safeAgentId);
      connection.setRequestProperty(RemoteCallHeaderConstants.BUNDLE_ID, safeBundleId);
      if (StringUtils.hasText(agentAlias)) {
        connection.setRequestProperty(RemoteCallHeaderConstants.AGENT_ALIAS, agentAlias.trim());
      }
      writeJson(connection, outboundRequest);
      int statusCode = connection.getResponseCode();
      if (statusCode < 200 || statusCode >= 300) {
        return agentFailure(
            safeAgentAlias,
            conversationId,
            startedAt,
            "Super智能体接口调用失败，HTTP " + statusCode + "：" + truncate(readAll(connection.getErrorStream()), 500));
      }
      return parseAgentStream(safeAgentAlias, conversationId, startedAt, connection.getInputStream());
    } catch (Exception e) {
      return agentFailure(safeAgentAlias, conversationId, startedAt, "Super智能体接口调用失败：" + e.getMessage());
    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
  }

  private AgentDefinition toAgentDefinition(SuperAgentInfo agent) {
    String id = firstNonBlank(agent.superAgentId(), agent.name());
    String versionId = firstNonBlank(agent.currentBundleId(), agent.bundleVersion(), id);
    String versionName = firstNonBlank(agent.bundleVersion(), agent.currentBundleId(), AgentFieldDisplayConstants.DEFAULT_VERSION_NAME);
    return new AgentDefinition(
        id,
        firstNonBlank(agent.displayName(), agent.name(), id),
        agent.description() == null ? "" : agent.description(),
        firstNonBlank(agent.iconUrl()),
        List.of(new AgentVersion(versionId, versionName)),
        List.of(),
        List.of(agentField(AgentInputFieldConstants.QUERY, AgentFieldDisplayConstants.QUERY_DESCRIPTION, 1)),
        List.of(
            agentField(AgentOutputFieldConstants.TEXT, AgentFieldDisplayConstants.TEXT_DESCRIPTION, 1),
            agentField(AgentOutputFieldConstants.REASONING, AgentFieldDisplayConstants.REASONING_DESCRIPTION, 2),
            agentField(AgentOutputFieldConstants.DEBUG, AgentFieldDisplayConstants.DEBUG_DESCRIPTION, 3),
            agentField(AgentOutputFieldConstants.ERROR, AgentFieldDisplayConstants.ERROR_DESCRIPTION, 4),
            agentField(AgentOutputFieldConstants.RAW_TEXT, AgentFieldDisplayConstants.RAW_TEXT_DESCRIPTION, 5)));
  }

  private AgentDefinition toAgentDefinition(SuperAgentDetail agent) {
    String id = firstNonBlank(agent.superAgentId(), agent.name());
    return new AgentDefinition(
        id,
        firstNonBlank(agent.displayName(), agent.name(), id),
        agent.description() == null ? "" : agent.description(),
        "",
        List.of(),
        normalizeChildAgents(agent.loadedAgents()),
        defaultAgentInputs(),
        defaultAgentOutputs());
  }

  private List<AgentVersion> normalizeAgentBundles(AgentBundleListResult result) {
    Map<String, AgentVersion> versions = new LinkedHashMap<>();
    if (result != null && result.items() != null) {
      for (AgentBundleItem item : result.items()) {
        addAgentBundle(versions, item);
      }
    }
    return List.copyOf(versions.values());
  }

  private void addAgentBundle(Map<String, AgentVersion> versions, AgentBundleItem item) {
    String bundleId = firstNonBlank(item == null ? null : item.bundleId());
    if (!StringUtils.hasText(bundleId) || versions.containsKey(bundleId)) {
      return;
    }
    versions.put(bundleId, new AgentVersion(bundleId, firstNonBlank(item.bundleVersion(), bundleId)));
  }

  private List<AgentChild> normalizeChildAgents(List<LoadedAgent> loadedAgents) {
    if (loadedAgents == null || loadedAgents.isEmpty()) {
      return List.of();
    }
    Map<String, AgentChild> children = new LinkedHashMap<>();
    for (LoadedAgent loadedAgent : loadedAgents) {
      if (loadedAgent == null || !StringUtils.hasText(loadedAgent.agentAlias())) {
        continue;
      }
      String alias = loadedAgent.agentAlias().trim();
      children.putIfAbsent(alias, new AgentChild(
          alias,
          firstNonBlank(loadedAgent.metaAgentName(), alias),
          firstNonBlank(loadedAgent.version()),
          firstNonBlank(loadedAgent.routePattern())));
    }
    return List.copyOf(children.values());
  }

  private List<AgentField> defaultAgentInputs() {
    return List.of(agentField(AgentInputFieldConstants.QUERY, AgentFieldDisplayConstants.QUERY_DESCRIPTION, 1));
  }

  private List<AgentField> defaultAgentOutputs() {
    return List.of(
        agentField(AgentOutputFieldConstants.TEXT, AgentFieldDisplayConstants.TEXT_DESCRIPTION, 1),
        agentField(AgentOutputFieldConstants.REASONING, AgentFieldDisplayConstants.REASONING_DESCRIPTION, 2),
        agentField(AgentOutputFieldConstants.DEBUG, AgentFieldDisplayConstants.DEBUG_DESCRIPTION, 3),
        agentField(AgentOutputFieldConstants.ERROR, AgentFieldDisplayConstants.ERROR_DESCRIPTION, 4),
        agentField(AgentOutputFieldConstants.RAW_TEXT, AgentFieldDisplayConstants.RAW_TEXT_DESCRIPTION, 5),
        agentField(AgentOutputFieldConstants.SKILL_TRIGGER, AgentFieldDisplayConstants.SKILL_TRIGGER_DESCRIPTION, 6),
        agentField(AgentOutputFieldConstants.REFERENCES, AgentFieldDisplayConstants.REFERENCES_DESCRIPTION, 7),
        agentField(AgentOutputFieldConstants.TOOL_CALL, AgentFieldDisplayConstants.TOOL_CALL_DESCRIPTION, 8),
        agentField(AgentOutputFieldConstants.TOOL_RESPONSE, AgentFieldDisplayConstants.TOOL_RESPONSE_DESCRIPTION, 9),
        agentField(AgentOutputFieldConstants.GEN_UI, AgentFieldDisplayConstants.GEN_UI_DESCRIPTION, 10));
  }

  private AgentField agentField(String fieldName, String description, int displayOrder) {
    return new AgentField(fieldName, fieldName, DatasetFieldTypeConstants.STRING, description, displayOrder);
  }

  private Map<String, String> chatCompletionHeaders() {
    Map<String, String> headers = new LinkedHashMap<>();
    headers.put(RemoteCallHeaderConstants.SPACE_ID, firstNonBlank(CurrentSpaceHolder.get()));
    String taskCookie = TaskCookieHolder.get();
    if (StringUtils.hasText(taskCookie)) {
      headers.put(RemoteCallHeaderConstants.COOKIE, taskCookie.trim());
    }
    return headers;
  }

  private HttpURLConnection openConnection(String url, String method) throws IOException {
    HttpURLConnection connection = (HttpURLConnection) remoteUri(url).toURL().openConnection();
    connection.setRequestMethod(method);
    connection.setConnectTimeout(Math.max(properties.getConnectTimeoutMs(), 1));
    connection.setReadTimeout(Math.max(properties.getReadTimeoutMs(), 1));
    if (RemoteCallProtocolConstants.POST.equalsIgnoreCase(method)) {
      connection.setDoOutput(true);
    }
    return connection;
  }

  private URI remoteUri(String url) {
    String safeUrl = requireText(url, "远程调用地址不能为空");
    URI uri = URI.create(safeUrl);
    if (RemoteCallProtocolConstants.HTTP.equalsIgnoreCase(uri.getScheme())
        || RemoteCallProtocolConstants.HTTPS.equalsIgnoreCase(uri.getScheme())) {
      return uri;
    } else {
      throw new IllegalStateException("远程调用地址仅支持HTTP或HTTPS：" + safeUrl);
    }
  }

  private void writeJson(HttpURLConnection connection, Object body) throws IOException {
    byte[] payload = objectMapper.writeValueAsBytes(body);
    connection.setFixedLengthStreamingMode(payload.length);
    try (var outputStream = connection.getOutputStream()) {
      outputStream.write(payload);
    }
  }

  private void ensureSuccess(String name, String status, Boolean success) {
    if (!RemoteCallStatusConstants.SUCCESS_CODE.equals(status) || Boolean.FALSE.equals(success)) {
      throw new IllegalStateException(name + "返回失败，status=" + status + "，success=" + success);
    }
  }

  private AgentChatResponse parseAgentStream(
      String agentAlias,
      String fallbackConversationId,
      long startedAt,
      InputStream inputStream
  ) throws IOException {
    RemoteAgentAggregate aggregate = new RemoteAgentAggregate(fallbackConversationId);
    if (inputStream == null) {
      return agentFailure(agentAlias, fallbackConversationId, startedAt, "Super智能体响应为空");
    }
    StringBuilder plainText = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        String payload = normalizeSsePayload(line);
        if (!StringUtils.hasText(payload) || RemoteCallProtocolConstants.SSE_DONE_PAYLOAD.equals(payload)) {
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
      aggregate.choices.add(choice(0, contentBlock(ChatContentTypeConstants.TEXT, plainText.toString())));
    }
    Map<String, String> outputs = buildAgentOutputs(aggregate.choices);
    String rawOutput = firstNonBlank(outputs.get(AgentOutputFieldConstants.RAW_TEXT), String.join("\n", aggregate.rawPayloads));
    String error = outputs.getOrDefault(AgentOutputFieldConstants.ERROR, "");
    return new AgentChatResponse(
        firstNonBlank(aggregate.id, UUID.randomUUID().toString().replace("-", "")),
        firstNonBlank(aggregate.conversationId, fallbackConversationId),
        aggregate.masterAgent,
        aggregate.metaAgent,
        firstNonBlank(aggregate.userId, ""),
        firstNonBlank(aggregate.object, ""),
        aggregate.created == null ? System.currentTimeMillis() : aggregate.created,
        firstNonBlank(aggregate.model, ""),
        aggregate.choices,
        StringUtils.hasText(error) ? RemoteCallStatusConstants.FAILED : RemoteCallStatusConstants.COMPLETED,
        outputs,
        System.currentTimeMillis() - startedAt,
        error,
        rawOutput);
  }

  private AgentChatResponse agentFailure(String agentAlias, String conversationId, long startedAt, String errorMessage) {
    List<Choice> choices = List.of(choice(0, errorBlock(errorMessage)));
    Map<String, String> outputs = buildAgentOutputs(choices);
    return new AgentChatResponse(
        UUID.randomUUID().toString().replace("-", ""),
        conversationId,
        agentAlias,
        null,
        "",
        "",
        System.currentTimeMillis(),
        "",
        choices,
        RemoteCallStatusConstants.FAILED,
        outputs,
        System.currentTimeMillis() - startedAt,
        errorMessage,
        errorMessage);
  }

  private void mergeAgentPayload(RemoteAgentAggregate aggregate, String payload) throws IOException {
    JsonNode root = objectMapper.readTree(payload);
    aggregate.id = firstNonBlank(textValue(root, RemoteCallJsonFieldConstants.ID), aggregate.id);
    aggregate.conversationId = firstNonBlank(textValue(root, RemoteCallJsonFieldConstants.CONVERSATION_ID), aggregate.conversationId);
    aggregate.masterAgent = firstNonBlank(textValue(root, RemoteCallJsonFieldConstants.MASTER_AGENT), aggregate.masterAgent);
    aggregate.metaAgent = firstNonBlank(textValue(root, RemoteCallJsonFieldConstants.META_AGENT), aggregate.metaAgent);
    aggregate.userId = firstNonBlank(textValue(root, RemoteCallJsonFieldConstants.USER_ID), aggregate.userId);
    aggregate.object = firstNonBlank(textValue(root, RemoteCallJsonFieldConstants.OBJECT), aggregate.object);
    aggregate.model = firstNonBlank(textValue(root, RemoteCallJsonFieldConstants.MODEL), aggregate.model);
    if (root.hasNonNull(RemoteCallJsonFieldConstants.CREATED)) {
      aggregate.created = root.get(RemoteCallJsonFieldConstants.CREATED).asLong();
    }
    JsonNode choicesNode = root.get(RemoteCallJsonFieldConstants.CHOICES);
    if (choicesNode != null && choicesNode.isArray()) {
      for (JsonNode choiceNode : choicesNode) {
        aggregate.choices.add(parseChoice(choiceNode, aggregate.choices.size()));
      }
    }
  }

  private Choice parseChoice(JsonNode choiceNode, int fallbackIndex) {
    Integer index = choiceNode != null && choiceNode.hasNonNull(RemoteCallJsonFieldConstants.INDEX)
        ? choiceNode.get(RemoteCallJsonFieldConstants.INDEX).asInt()
        : fallbackIndex;
    String finishReason = textValue(choiceNode, RemoteCallJsonFieldConstants.FINISH_REASON);
    JsonNode deltaNode = choiceNode == null ? null : choiceNode.get(RemoteCallJsonFieldConstants.DELTA);
    String role = firstNonBlank(textValue(deltaNode, RemoteCallJsonFieldConstants.ROLE), ChatRoleConstants.ASSISTANT);
    List<DeltaContent> contents = parseDeltaContents(deltaNode == null ? null : deltaNode.get(RemoteCallJsonFieldConstants.CONTENT));
    List<ToolCallDelta> toolCalls = null;
    JsonNode toolCallsNode = deltaNode == null ? null : deltaNode.get(RemoteCallJsonFieldConstants.TOOL_CALLS);
    if (toolCallsNode != null && !toolCallsNode.isNull()) {
      toolCalls = objectMapper.convertValue(toolCallsNode, TOOL_CALLS_TYPE);
    }
    Map<String, Object> extra = null;
    JsonNode extraNode = deltaNode == null ? null : deltaNode.get(RemoteCallJsonFieldConstants.EXTRA);
    if (extraNode != null && !extraNode.isNull()) {
      extra = objectMapper.convertValue(extraNode, EXTRA_TYPE);
    }
    return new Choice(index, new Delta(role, contents, toolCalls, extra), finishReason);
  }

  private List<DeltaContent> parseDeltaContents(JsonNode contentNode) {
    if (contentNode == null || contentNode.isNull()) {
      return List.of();
    }
    List<DeltaContent> contents = new ArrayList<>();
    if (contentNode.isArray()) {
      for (JsonNode item : contentNode) {
        contents.add(parseDeltaContent(item));
      }
    } else {
      contents.add(parseDeltaContent(contentNode));
    }
    return contents;
  }

  private DeltaContent parseDeltaContent(JsonNode item) {
    if (item == null || item.isNull()) {
      return new TextContent("");
    }
    if (item.isTextual()) {
      return new TextContent(item.asText());
    }
    String type = firstNonBlank(textValue(item, RemoteCallJsonFieldConstants.TYPE), ChatContentTypeConstants.TEXT);
    String text = textValue(item, RemoteCallJsonFieldConstants.TEXT);
    String reasoning = textValue(item, ChatContentTypeConstants.REASONING);
    String error = textValue(item, ChatContentTypeConstants.ERROR);
    String skillName = firstNonBlank(
        textValue(item, RemoteCallJsonFieldConstants.SKILL_NAME),
        textValue(item, RemoteCallJsonFieldConstants.SKILL_NAME_SNAKE));
    String skillDesc = firstNonBlank(
        textValue(item, RemoteCallJsonFieldConstants.SKILL_DESC),
        textValue(item, RemoteCallJsonFieldConstants.SKILL_DESC_SNAKE));
    String toolCallId = firstNonBlank(
        textValue(item, RemoteCallJsonFieldConstants.TOOL_CALL_ID),
        textValue(item, RemoteCallJsonFieldConstants.TOOL_CALL_ID_SNAKE));
    String toolName = firstNonBlank(
        textValue(item, RemoteCallJsonFieldConstants.TOOL_NAME),
        textValue(item, RemoteCallJsonFieldConstants.TOOL_NAME_SNAKE));
    String arguments = textValue(item, RemoteCallJsonFieldConstants.ARGUMENTS);
    String response = textValue(item, RemoteCallJsonFieldConstants.RESPONSE);
    List<ReferenceItem> references = parseReferences(item.get(RemoteCallJsonFieldConstants.REFERENCES));
    UICardDefinition uiCardDefinition = parseUiCardDefinition(item);
    String normalizedType = normalizeAgentContentType(type.trim());
    String fallbackValue = firstNonEmpty(text, reasoning, error, firstNonTypeFieldValue(item));
    return switch (normalizedType) {
      case ChatContentTypeConstants.REASONING -> new ReasoningContent(firstNonEmpty(reasoning, text, fallbackValue));
      case ChatContentTypeConstants.DEBUG -> new DebugContent(firstNonEmpty(text, fallbackValue));
      case ChatContentTypeConstants.ERROR -> new ErrorContent(firstNonEmpty(error, text, fallbackValue));
      case ChatContentTypeConstants.SKILL_TRIGGER -> new SkillTriggerContent(skillName, skillDesc);
      case ChatContentTypeConstants.REFERENCES -> new ReferencesContent(references);
      case ChatContentTypeConstants.TOOL_CALL -> new ToolCallContent(toolCallId, toolName, arguments);
      case ChatContentTypeConstants.TOOL_RESPONSE -> new ToolResponseContent(toolCallId, toolName, response);
      case ChatContentTypeConstants.GEN_UI -> new GenUIContent(uiCardDefinition);
      default -> new TextContent(firstNonEmpty(text, fallbackValue));
    };
  }

  private List<ReferenceItem> parseReferences(JsonNode referencesNode) {
    if (referencesNode == null || referencesNode.isNull()) {
      return List.of();
    }
    if (!referencesNode.isArray()) {
      return List.of(objectMapper.convertValue(referencesNode, ReferenceItem.class));
    }
    return objectMapper.convertValue(referencesNode, REFERENCES_TYPE);
  }

  private UICardDefinition parseUiCardDefinition(JsonNode item) {
    JsonNode uiCardNode = item == null ? null : item.get(RemoteCallJsonFieldConstants.UI_CARD_DEFINITION_LEGACY);
    if (uiCardNode == null || uiCardNode.isNull()) {
      uiCardNode = item == null ? null : item.get(RemoteCallJsonFieldConstants.UI_CARD_DEFINITION);
    }
    if (uiCardNode == null || uiCardNode.isNull()) {
      uiCardNode = item == null ? null : item.get(RemoteCallJsonFieldConstants.UI_CARD_DEFINITION_SNAKE);
    }
    if (uiCardNode == null || uiCardNode.isNull()) {
      return null;
    }
    return objectMapper.convertValue(uiCardNode, UICardDefinition.class);
  }

  private Map<String, String> buildAgentOutputs(List<Choice> choices) {
    AgentOutputParts parts = new AgentOutputParts();
    for (Choice choice : choices) {
      if (choice == null || choice.getDelta() == null) {
        continue;
      }
      if (choice.getDelta().getContent() != null) {
        for (DeltaContent content : choice.getDelta().getContent()) {
          appendContentPart(parts, content);
        }
      }
      appendToolCalls(parts.toolCallParts, choice.getDelta().getTool_calls());
    }
    Map<String, String> outputs = new LinkedHashMap<>();
    putIfText(outputs, AgentOutputFieldConstants.DEBUG, joinStreamParts(parts.debugParts));
    putIfText(outputs, AgentOutputFieldConstants.REASONING, joinStreamParts(parts.reasoningParts));
    putIfText(outputs, AgentOutputFieldConstants.TEXT, joinStreamParts(parts.textParts));
    putIfText(outputs, AgentOutputFieldConstants.ERROR, joinStreamParts(parts.errorParts));
    putIfText(outputs, AgentOutputFieldConstants.SKILL_TRIGGER, joinNonBlank("\n", parts.skillTriggerParts.toArray(String[]::new)));
    putIfText(outputs, AgentOutputFieldConstants.REFERENCES, joinNonBlank("\n", parts.referenceParts.toArray(String[]::new)));
    putIfText(outputs, AgentOutputFieldConstants.TOOL_CALL, joinNonBlank("\n", parts.toolCallParts.toArray(String[]::new)));
    putIfText(outputs, AgentOutputFieldConstants.TOOL_RESPONSE, joinNonBlank("\n", parts.toolResponseParts.toArray(String[]::new)));
    putIfText(outputs, AgentOutputFieldConstants.GEN_UI, joinNonBlank("\n", parts.genUiParts.toArray(String[]::new)));
    putIfText(outputs, AgentOutputFieldConstants.ANSWER, firstNonBlank(outputs.get(AgentOutputFieldConstants.TEXT)));
    putIfText(outputs, AgentOutputFieldConstants.CONTENT, firstNonBlank(outputs.get(AgentOutputFieldConstants.TEXT)));
    putIfText(outputs, AgentOutputFieldConstants.RAW_TEXT, joinNonBlank(
        "\n",
        outputs.get(AgentOutputFieldConstants.DEBUG),
        outputs.get(AgentOutputFieldConstants.REASONING),
        outputs.get(AgentOutputFieldConstants.TEXT),
        outputs.get(AgentOutputFieldConstants.SKILL_TRIGGER),
        outputs.get(AgentOutputFieldConstants.REFERENCES),
        outputs.get(AgentOutputFieldConstants.TOOL_CALL),
        outputs.get(AgentOutputFieldConstants.TOOL_RESPONSE),
        outputs.get(AgentOutputFieldConstants.GEN_UI),
        outputs.get(AgentOutputFieldConstants.ERROR)));
    return outputs;
  }

  private void appendContentPart(AgentOutputParts parts, DeltaContent content) {
    if (content == null || !StringUtils.hasText(content.getType())) {
      return;
    }
    String type = content.getType().trim();
    String value = contentDisplayValue(content);
    if (value.isEmpty()) {
      return;
    }
    if (ChatContentTypeConstants.DEBUG.equals(type)) {
      parts.debugParts.add(value);
    } else if (ChatContentTypeConstants.REASONING.equals(type)) {
      parts.reasoningParts.add(value);
    } else if (ChatContentTypeConstants.TEXT.equals(type)) {
      parts.textParts.add(value);
    } else if (ChatContentTypeConstants.ERROR.equals(type)) {
      parts.errorParts.add(value);
    } else if (ChatContentTypeConstants.SKILL_TRIGGER.equals(type)) {
      parts.skillTriggerParts.add(value);
    } else if (ChatContentTypeConstants.REFERENCES.equals(type)) {
      parts.referenceParts.add(value);
    } else if (ChatContentTypeConstants.TOOL_CALL.equals(type)) {
      parts.toolCallParts.add(value);
    } else if (ChatContentTypeConstants.TOOL_RESPONSE.equals(type)) {
      parts.toolResponseParts.add(value);
    } else if (ChatContentTypeConstants.GEN_UI.equals(type)) {
      parts.genUiParts.add(value);
    } else {
      parts.textParts.add(value);
    }
  }

  private String contentDisplayValue(DeltaContent content) {
    if (content instanceof SkillTriggerContent skillTriggerContent) {
      return joinNonBlank(" - ", skillTriggerContent.getSkillName(), skillTriggerContent.getSkillDesc());
    } else if (content instanceof ReferencesContent referencesContent) {
      return toJson(referencesContent.getReferences());
    } else if (content instanceof ToolCallContent toolCallContent) {
      return toJson(Map.of(
          RemoteCallJsonFieldConstants.TOOL_CALL_ID, firstNonBlank(toolCallContent.getToolCallId()),
          RemoteCallJsonFieldConstants.TOOL_NAME, firstNonBlank(toolCallContent.getToolName()),
          RemoteCallJsonFieldConstants.ARGUMENTS, firstNonBlank(toolCallContent.getArguments())));
    } else if (content instanceof ToolResponseContent toolResponseContent) {
      return toJson(Map.of(
          RemoteCallJsonFieldConstants.TOOL_CALL_ID, firstNonBlank(toolResponseContent.getToolCallId()),
          RemoteCallJsonFieldConstants.TOOL_NAME, firstNonBlank(toolResponseContent.getToolName()),
          RemoteCallJsonFieldConstants.RESPONSE, firstNonBlank(toolResponseContent.getResponse())));
    } else if (content instanceof GenUIContent genUIContent) {
      return toJson(genUIContent.getUiCardDefinition());
    } else if (content instanceof DebugContent debugContent) {
      return firstNonEmpty(debugContent.getText());
    } else if (content instanceof ReasoningContent reasoningContent) {
      return firstNonEmpty(reasoningContent.getReasoning());
    } else if (content instanceof ErrorContent errorContent) {
      return firstNonEmpty(errorContent.getError());
    } else if (content instanceof TextContent textContent) {
      return firstNonEmpty(textContent.getText());
    } else {
      return toJson(content);
    }
  }

  private void appendToolCalls(List<String> toolCallParts, List<ToolCallDelta> toolCalls) {
    if (toolCalls == null || toolCalls.isEmpty()) {
      return;
    }
    for (ToolCallDelta toolCall : toolCalls) {
      String value = toJson(toolCall);
      if (StringUtils.hasText(value)) {
        toolCallParts.add(value);
      }
    }
  }

  private String toJson(Object value) {
    if (value == null) {
      return "";
    }
    try {
      return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
    } catch (Exception e) {
      return String.valueOf(value);
    }
  }

  private Choice choice(Integer index, DeltaContent content) {
    return new Choice(
        index,
        new Delta(ChatRoleConstants.ASSISTANT, content == null ? List.of() : List.of(content), null, null),
        null);
  }

  private DeltaContent contentBlock(String type, String text) {
    if (ChatContentTypeConstants.ERROR.equals(type)) {
      return new ErrorContent(text);
    } else {
      return new TextContent(text);
    }
  }

  private DeltaContent errorBlock(String error) {
    return new ErrorContent(error);
  }

  private String normalizeAgentContentType(String type) {
    if (isContentType(type, ChatContentTypeConstants.SKILL_TRIGGER, ChatContentTypeConstants.SKILL_TRIGGER_CAMEL)) {
      return ChatContentTypeConstants.SKILL_TRIGGER;
    } else if (isContentType(type, ChatContentTypeConstants.TOOL_CALL, ChatContentTypeConstants.TOOL_CALL_CAMEL)) {
      return ChatContentTypeConstants.TOOL_CALL;
    } else if (isContentType(type, ChatContentTypeConstants.TOOL_RESPONSE, ChatContentTypeConstants.TOOL_RESPONSE_CAMEL)) {
      return ChatContentTypeConstants.TOOL_RESPONSE;
    } else if (isContentType(type, ChatContentTypeConstants.GEN_UI, ChatContentTypeConstants.GEN_UI_CAMEL)) {
      return ChatContentTypeConstants.GEN_UI;
    } else {
      return type;
    }
  }

  private boolean isContentType(String actualType, String snakeCaseType, String camelCaseType) {
    return snakeCaseType.equals(actualType) || camelCaseType.equals(actualType);
  }

  private String normalizeSsePayload(String line) {
    if (!StringUtils.hasText(line)) {
      return "";
    }
    String trimmed = line.trim();
    if (trimmed.startsWith(RemoteCallProtocolConstants.SSE_COMMENT_PREFIX)
        || trimmed.startsWith(RemoteCallProtocolConstants.SSE_EVENT_PREFIX)
        || trimmed.startsWith(RemoteCallProtocolConstants.SSE_ID_PREFIX)
        || trimmed.startsWith(RemoteCallProtocolConstants.SSE_RETRY_PREFIX)) {
      return "";
    }
    if (trimmed.startsWith(RemoteCallProtocolConstants.SSE_DATA_PREFIX)) {
      return trimmed.substring(RemoteCallProtocolConstants.SSE_DATA_PREFIX.length()).trim();
    }
    return trimmed;
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

  private String firstNonTypeFieldValue(JsonNode item) {
    if (item == null || !item.isObject()) {
      return "";
    }
    var fields = item.fields();
    while (fields.hasNext()) {
      Map.Entry<String, JsonNode> entry = fields.next();
      if (!RemoteCallJsonFieldConstants.TYPE.equals(entry.getKey())) {
        String value = scalarText(entry.getValue());
        if (StringUtils.hasText(value)) {
          return value;
        }
      }
    }
    return "";
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

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (StringUtils.hasText(value)) {
        return value.trim();
      }
    }
    return "";
  }

  private String firstNonEmpty(String... values) {
    for (String value : values) {
      if (value != null && !value.isEmpty()) {
        return value;
      }
    }
    return "";
  }

  private void putIfText(Map<String, String> outputs, String key, String value) {
    if (StringUtils.hasText(value)) {
      outputs.put(key, value);
    }
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

  private String joinStreamParts(List<String> parts) {
    if (parts == null || parts.isEmpty()) {
      return "";
    }
    StringBuilder result = new StringBuilder();
    for (String part : parts) {
      if (part != null && !part.isEmpty()) {
        result.append(part);
      }
    }
    return result.toString();
  }

  private String requireText(String value, String message) {
    if (!StringUtils.hasText(value)) {
      throw new IllegalStateException(message);
    }
    return value.trim();
  }

  private String truncate(String value, int maxLength) {
    if (value == null || value.length() <= maxLength) {
      return value == null ? "" : value;
    }
    return value.substring(0, maxLength);
  }

  private static final class AgentOutputParts {
    private final List<String> debugParts = new ArrayList<>();
    private final List<String> reasoningParts = new ArrayList<>();
    private final List<String> textParts = new ArrayList<>();
    private final List<String> errorParts = new ArrayList<>();
    private final List<String> skillTriggerParts = new ArrayList<>();
    private final List<String> referenceParts = new ArrayList<>();
    private final List<String> toolCallParts = new ArrayList<>();
    private final List<String> toolResponseParts = new ArrayList<>();
    private final List<String> genUiParts = new ArrayList<>();
  }

  private static class RemoteAgentAggregate {
    private String id = "";
    private String conversationId;
    private String masterAgent = "";
    private String metaAgent = "";
    private String userId = "";
    private String object = "";
    private Long created;
    private String model = "";
    private final List<Choice> choices = new ArrayList<>();
    private final List<String> rawPayloads = new ArrayList<>();

    private RemoteAgentAggregate(String conversationId) {
      this.conversationId = conversationId;
    }
  }
}
