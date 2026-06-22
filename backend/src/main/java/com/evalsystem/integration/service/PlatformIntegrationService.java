package com.evalsystem.integration.service;

import com.evalsystem.integration.config.PlatformIntegrationProperties;
import com.evalsystem.integration.api.dto.request.PlatformAgentChatRequest;
import com.evalsystem.integration.api.dto.response.PlatformAgentChild;
import com.evalsystem.integration.api.dto.response.PlatformAgentChatResponse;
import com.evalsystem.integration.api.dto.response.PlatformAgentChoice;
import com.evalsystem.integration.api.dto.response.PlatformAgentContentBlock;
import com.evalsystem.integration.api.dto.response.PlatformAgentDefinition;
import com.evalsystem.integration.api.dto.response.PlatformAgentDetailResponse;
import com.evalsystem.integration.api.dto.response.PlatformAgentDelta;
import com.evalsystem.integration.api.dto.response.PlatformAgentField;
import com.evalsystem.integration.api.dto.response.PlatformAgentInstance;
import com.evalsystem.integration.api.dto.response.PlatformAgentListResponse;
import com.evalsystem.integration.api.dto.response.PlatformAgentReferenceItem;
import com.evalsystem.integration.api.dto.request.PlatformAgentMessage;
import com.evalsystem.integration.api.dto.response.PlatformAgentToolCallDelta;
import com.evalsystem.integration.api.dto.response.PlatformAgentUiCardDefinition;
import com.evalsystem.integration.api.dto.response.PlatformAgentVersion;
import com.evalsystem.integration.api.dto.response.PlatformLoadedAgent;
import com.evalsystem.integration.api.dto.response.PlatformListResult;
import com.evalsystem.integration.api.dto.request.PlatformLoginRequest;
import com.evalsystem.integration.api.dto.response.PlatformLoginResponse;
import com.evalsystem.integration.api.dto.request.PlatformModelChatRequest;
import com.evalsystem.integration.api.dto.response.PlatformModelChatResponse;
import com.evalsystem.integration.api.dto.response.PlatformModelChatResult;
import com.evalsystem.integration.api.dto.response.PlatformModelInfo;
import com.evalsystem.integration.api.dto.response.PlatformModelListResponse;
import com.evalsystem.integration.api.dto.response.PlatformSuperAgentDetail;
import com.evalsystem.integration.api.dto.response.PlatformSuperAgentInfo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PlatformIntegrationService {
  private static final String STATUS_COMPLETED = "completed";
  private static final String STATUS_FAILED = "failed";
  private static final String ROLE_ASSISTANT = "assistant";
  private static final String IAM_AUTH_TYPE = "IAM";
  private static final String THINK_END_TAG = "</think>";
  private static final String DEFAULT_AGENT_ALIAS = "router-agent";
  private static final String RESPONSE_OBJECT = "com.evalsystem.integration.api.dto.response.PlatformAgentChatResponse";
  private static final HostnameVerifier TRUST_ALL_HOSTNAME_VERIFIER = (hostname, session) -> true;
  private static final TypeReference<List<PlatformAgentToolCallDelta>> TOOL_CALLS_TYPE = new TypeReference<>() {
  };
  private static final TypeReference<List<PlatformAgentReferenceItem>> REFERENCES_TYPE = new TypeReference<>() {
  };
  private static final TypeReference<Map<String, Object>> EXTRA_TYPE = new TypeReference<>() {
  };

  private final PlatformIntegrationProperties properties;
  private final ObjectMapper objectMapper;
  private static volatile SSLSocketFactory trustAllSocketFactory;
  private String cookieCache = "";

  public PlatformIntegrationService(PlatformIntegrationProperties properties, ObjectMapper objectMapper) {
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  public List<PlatformModelInfo> listModels() {
    requireText(properties.getModelListUrl(), "请配置模型列表接口 integration.platform.model-list-url");
    PlatformModelListResponse response = exchangeJson(
        "GET",
        properties.getModelListUrl(),
        null,
        authHeaders(true),
        PlatformModelListResponse.class);
    ensureSuccess("模型列表接口", response.status(), response.success());
    PlatformListResult<PlatformModelInfo> result = response.resultObjVO();
    List<PlatformModelInfo> models = result == null || result.list() == null ? List.of() : result.list();
    if (!properties.getIam().isEnabled()) {
      return models;
    }
    return models.stream()
        .filter(model -> IAM_AUTH_TYPE.equalsIgnoreCase(model.authType()))
        .toList();
  }

  public List<PlatformAgentDefinition> listAgents() {
    requireText(properties.getAgentListUrl(), "请配置智能体列表接口 integration.platform.agent-list-url");
    PlatformAgentListResponse response = exchangeJson(
        "GET",
        properties.getAgentListUrl(),
        null,
        authHeaders(true),
        PlatformAgentListResponse.class);
    ensureSuccess("智能体列表接口", response.status(), response.success());
    PlatformListResult<PlatformSuperAgentInfo> result = response.resultObjVO();
    if (result == null || result.list() == null) {
      return List.of();
    }
    return result.list().stream().map(this::toAgentDefinition).toList();
  }

  public PlatformAgentDefinition getAgentDetail(String agentId) {
    return toAgentDefinition(loadAgentDetail(agentId));
  }

  private PlatformSuperAgentDetail loadAgentDetail(String agentId) {
    String safeAgentId = requireText(agentId, "Agent ID cannot be blank");
    requireText(properties.getAgentDetailUrl(), "Please configure agent detail API integration.platform.agent-detail-url");
    PlatformAgentDetailResponse response = exchangeJson(
        "GET",
        agentDetailUrl(safeAgentId),
        null,
        authHeaders(true),
        PlatformAgentDetailResponse.class);
    ensureSuccess("Agent detail API", response.status(), response.success());
    if (response.resultObjVO() == null) {
      throw new IllegalStateException("Agent detail API returned empty result");
    }
    return response.resultObjVO();
  }

  public PlatformModelChatResult chatModel(String modelId, String message) {
    if (properties.getIam().isEnabled()) {
      return chatIamModel(modelId, message);
    }
    requireText(modelId, "模型ID不能为空");
    requireText(properties.getModelChatUrl(), "请配置模型对话接口 integration.platform.model-chat-url");
    PlatformModelChatResponse response = exchangeJson(
        "POST",
        modelChatUrl(modelId),
        new PlatformModelChatRequest(message == null ? "" : message),
        authHeaders(true),
        PlatformModelChatResponse.class);
    ensureSuccess("模型对话接口", response.status(), response.success());
    if (response.resultObjVO() == null) {
      throw new IllegalStateException("模型对话接口返回为空");
    }
    return response.resultObjVO();
  }

  private PlatformModelChatResult chatIamModel(String modelId, String message) {
    requireText(modelId, "模型ID不能为空");
    requireText(properties.getIam().getUrl(), "请配置IAM模型对话接口 integration.platform.iam.url");
    requireText(properties.getIam().getAuthorization(), "请配置IAM模型Authorization integration.platform.iam.authorization");
    PlatformModelInfo model = findIamModel(modelId);
    String modelName = requireText(model.modelName(), "IAM模型缺少modelName：" + modelId);
    HttpURLConnection connection = null;
    try {
      connection = openConnection(properties.getIam().getUrl(), "POST");
      connection.setRequestProperty("Accept", "application/json");
      connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
      connection.setRequestProperty("Authorization", properties.getIam().getAuthorization());
      Map<String, Object> body = new LinkedHashMap<>();
      body.put("model", modelName);
      body.put("messages", List.of(Map.of(
          "role", "user",
          "content", message == null ? "" : message)));
      body.put("stream", Boolean.FALSE);
      writeJson(connection, body);
      int statusCode = connection.getResponseCode();
      String responseBody = readAll(statusCode >= 200 && statusCode < 300 ? connection.getInputStream() : connection.getErrorStream());
      if (statusCode < 200 || statusCode >= 300) {
        throw new IllegalStateException("IAM模型对话接口调用失败，HTTP " + statusCode + "：" + truncate(responseBody, 500));
      }
      String outputText = parseIamModelOutput(responseBody);
      return new PlatformModelChatResult(modelId, outputText, String.valueOf(System.currentTimeMillis()));
    } catch (IOException e) {
      throw new IllegalStateException("IAM模型对话接口调用失败：" + e.getMessage(), e);
    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
  }

  private PlatformModelInfo findIamModel(String modelId) {
    return listModels().stream()
        .filter(model -> modelId.equals(model.modelId()))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("IAM模型不存在或未启用：" + modelId));
  }

  private String parseIamModelOutput(String responseBody) throws IOException {
    JsonNode root = objectMapper.readTree(responseBody);
    JsonNode choicesNode = root.get("choices");
    if (choicesNode == null || !choicesNode.isArray() || choicesNode.isEmpty()) {
      throw new IllegalStateException("IAM模型对话接口返回缺少choices");
    }
    JsonNode messageNode = choicesNode.get(0).get("message");
    String content = textValue(messageNode, "content");
    if (!StringUtils.hasText(content)) {
      throw new IllegalStateException("IAM模型对话接口返回缺少message.content");
    }
    return cleanThinkContent(content);
  }

  private String cleanThinkContent(String content) {
    String safeContent = content == null ? "" : content;
    int thinkEnd = safeContent.indexOf(THINK_END_TAG);
    if (thinkEnd >= 0) {
      return safeContent.substring(thinkEnd + THINK_END_TAG.length()).trim();
    }
    return safeContent.trim();
  }

  public PlatformAgentChatResponse invokeAgent(String agentId, PlatformAgentChatRequest request) {
    return invokeAgent(agentId, "", request);
  }

  public PlatformAgentChatResponse invokeAgent(String agentId, String agentAlias, PlatformAgentChatRequest request) {
    String safeAgentId = firstNonBlank(agentId, DEFAULT_AGENT_ALIAS);
    String safeAgentAlias = firstNonBlank(agentAlias, safeAgentId);
    long startedAt = System.currentTimeMillis();
    String conversationId = StringUtils.hasText(request == null ? null : request.conversationId())
        ? request.conversationId()
        : UUID.randomUUID().toString().replace("-", "");
    PlatformAgentChatRequest outboundRequest = new PlatformAgentChatRequest(
        conversationId,
        request == null || request.messages() == null ? List.of() : request.messages(),
        Boolean.TRUE);
    PlatformSuperAgentDetail agentDetail = loadAgentDetail(safeAgentId);
    String chatUrl = agentChatUrl(agentDetail.accessUrl());

    for (int attempt = 0; attempt < 2; attempt++) {
      HttpURLConnection connection = null;
      try {
        connection = openConnection(chatUrl, "POST");
        connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
        connection.setRequestProperty("Accept", "text/event-stream, application/json");
        connection.setRequestProperty("Cookie", ensureCookie());
        if (StringUtils.hasText(agentAlias)) {
          connection.setRequestProperty("x-agent-alias", agentAlias.trim());
        }
        writeJson(connection, outboundRequest);
        int statusCode = connection.getResponseCode();
        if (isUnauthorized(statusCode) && attempt == 0) {
          readAll(connection.getErrorStream());
          refreshCookie();
          continue;
        }
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
    return agentFailure(safeAgentAlias, conversationId, startedAt, "Super智能体接口认证失败");
  }

  private PlatformAgentDefinition toAgentDefinition(PlatformSuperAgentInfo agent) {
    String id = firstNonBlank(agent.superAgentId(), agent.name());
    String versionId = firstNonBlank(agent.currentBundleId(), agent.bundleVersion(), id);
    String versionName = firstNonBlank(agent.bundleVersion(), agent.currentBundleId(), "当前版本");
    return new PlatformAgentDefinition(
        id,
        firstNonBlank(agent.displayName(), agent.name(), id),
        agent.description() == null ? "" : agent.description(),
        List.of(new PlatformAgentVersion(versionId, versionName)),
        List.of(),
        List.of(new PlatformAgentField("query", "query", "string", "用户输入或问题", 1)),
        List.of(
            new PlatformAgentField("text", "text", "string", "返回给用户的信息", 1),
            new PlatformAgentField("reasoning", "reasoning", "string", "智能体思考过程", 2),
            new PlatformAgentField("debug", "debug", "string", "智能体调试信息", 3),
            new PlatformAgentField("error", "error", "string", "智能体错误信息", 4),
            new PlatformAgentField("rawText", "rawText", "string", "消息合并后的原始文本", 5)));
  }

  private PlatformAgentDefinition toAgentDefinition(PlatformSuperAgentDetail agent) {
    String id = firstNonBlank(agent.superAgentId(), agent.name());
    return new PlatformAgentDefinition(
        id,
        firstNonBlank(agent.displayName(), agent.name(), id),
        agent.description() == null ? "" : agent.description(),
        normalizeAgentVersions(agent),
        normalizeChildAgents(agent.loadedAgents()),
        defaultAgentInputs(),
        defaultAgentOutputs());
  }

  private List<PlatformAgentVersion> normalizeAgentVersions(PlatformSuperAgentDetail agent) {
    Map<String, PlatformAgentVersion> versions = new LinkedHashMap<>();
    addAgentVersion(versions, firstNonBlank(agent.bundleVersion(), agent.currentBundleId()));
    if (agent.instances() != null) {
      for (PlatformAgentInstance instance : agent.instances()) {
        if (instance != null) {
          addAgentVersion(versions, instance.bundleVersion());
        }
      }
    }
    return List.copyOf(versions.values());
  }

  private void addAgentVersion(Map<String, PlatformAgentVersion> versions, String version) {
    String safeVersion = firstNonBlank(version);
    if (!StringUtils.hasText(safeVersion) || versions.containsKey(safeVersion)) {
      return;
    }
    versions.put(safeVersion, new PlatformAgentVersion(safeVersion, safeVersion));
  }

  private List<PlatformAgentChild> normalizeChildAgents(List<PlatformLoadedAgent> loadedAgents) {
    if (loadedAgents == null || loadedAgents.isEmpty()) {
      return List.of();
    }
    Map<String, PlatformAgentChild> children = new LinkedHashMap<>();
    for (PlatformLoadedAgent loadedAgent : loadedAgents) {
      if (loadedAgent == null || !StringUtils.hasText(loadedAgent.agentAlias())) {
        continue;
      }
      String alias = loadedAgent.agentAlias().trim();
      children.putIfAbsent(alias, new PlatformAgentChild(
          alias,
          firstNonBlank(loadedAgent.metaAgentName(), alias),
          firstNonBlank(loadedAgent.version()),
          firstNonBlank(loadedAgent.routePattern())));
    }
    return List.copyOf(children.values());
  }

  private List<PlatformAgentField> defaultAgentInputs() {
    return List.of(new PlatformAgentField("query", "query", "string", "User input or question", 1));
  }

  private List<PlatformAgentField> defaultAgentOutputs() {
    return List.of(
        new PlatformAgentField("text", "text", "string", "Agent answer", 1),
        new PlatformAgentField("reasoning", "reasoning", "string", "Agent reasoning", 2),
        new PlatformAgentField("debug", "debug", "string", "Agent debug information", 3),
        new PlatformAgentField("error", "error", "string", "Agent error information", 4),
        new PlatformAgentField("rawText", "rawText", "string", "Merged raw response text", 5),
        new PlatformAgentField("skillTrigger", "skillTrigger", "string", "Triggered skill metadata", 6),
        new PlatformAgentField("references", "references", "string", "Reference list", 7),
        new PlatformAgentField("toolCall", "toolCall", "string", "Tool call messages", 8),
        new PlatformAgentField("toolResponse", "toolResponse", "string", "Tool response messages", 9),
        new PlatformAgentField("genUi", "genUi", "string", "Generated UI card definition", 10));
  }

  private Map<String, String> authHeaders(boolean includeSpaceId) {
    Map<String, String> headers = new LinkedHashMap<>();
    headers.put("Cookie", ensureCookie());
    if (includeSpaceId && StringUtils.hasText(properties.getXSpaceId())) {
      headers.put("x-space-id", properties.getXSpaceId());
    }
    return headers;
  }

  private synchronized String ensureCookie() {
    if (StringUtils.hasText(cookieCache)) {
      return cookieCache;
    }
    requireText(properties.getLogin().getUrl(), "请配置登录接口 integration.platform.login.url");
    HttpURLConnection connection = null;
    try {
      connection = openConnection(properties.getLogin().getUrl(), "POST");
      connection.setRequestProperty("Content-Type", "application/json");
      connection.setRequestProperty("Accept-Charset", "UTF-8");
      writeJson(connection, new PlatformLoginRequest(
          properties.getLogin().getLoginAccount(),
          properties.getLogin().getUid(),
          properties.getLogin().getPassword(),
          properties.getLogin().getLang(),
          properties.getLogin().getRememberAccountName(),
          properties.getLogin().getAppId(),
          properties.getLogin().getEncryptedPasswordSwitch()));
      int statusCode = connection.getResponseCode();
      String responseBody = readAll(statusCode >= 200 && statusCode < 300 ? connection.getInputStream() : connection.getErrorStream());
      if (statusCode < 200 || statusCode >= 300) {
        throw new IllegalStateException("登录接口调用失败，HTTP " + statusCode + "：" + truncate(responseBody, 500));
      }
      PlatformLoginResponse loginResponse = objectMapper.readValue(responseBody, PlatformLoginResponse.class);
      if (loginResponse.statusCode() == null || loginResponse.statusCode() != 0) {
        throw new IllegalStateException("登录失败：" + firstNonBlank(loginResponse.statusText(), responseBody));
      }
      String cookie = extractCookie(connection.getHeaderFields());
      if (!StringUtils.hasText(cookie)) {
        throw new IllegalStateException("登录成功但响应头中没有Set-Cookie");
      }
      cookieCache = cookie;
      return cookieCache;
    } catch (IOException e) {
      throw new IllegalStateException("登录接口调用失败：" + e.getMessage(), e);
    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
  }

  private synchronized String refreshCookie() {
    cookieCache = "";
    return ensureCookie();
  }

  private String extractCookie(Map<String, List<String>> headers) {
    if (headers == null || headers.isEmpty()) {
      return "";
    }
    List<String> cookies = new ArrayList<>();
    headers.forEach((key, values) -> {
      if (key != null && "Set-Cookie".equalsIgnoreCase(key) && values != null) {
        values.stream().filter(StringUtils::hasText).forEach(cookies::add);
      }
    });
    return String.join("; ", cookies);
  }

  private <T> T exchangeJson(String method, String url, Object body, Map<String, String> headers, Class<T> responseType) {
    Map<String, String> requestHeaders = headers == null ? new LinkedHashMap<>() : new LinkedHashMap<>(headers);
    for (int attempt = 0; attempt < 2; attempt++) {
      HttpURLConnection connection = null;
      try {
        connection = openConnection(url, method);
        connection.setRequestProperty("Accept", "application/json");
        if (!requestHeaders.isEmpty()) {
          requestHeaders.forEach(connection::setRequestProperty);
        }
        if (body != null) {
          connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
          writeJson(connection, body);
        }
        int statusCode = connection.getResponseCode();
        String responseBody = readAll(statusCode >= 200 && statusCode < 300 ? connection.getInputStream() : connection.getErrorStream());
        if (isUnauthorized(statusCode) && requestHeaders.containsKey("Cookie") && attempt == 0) {
          requestHeaders.put("Cookie", refreshCookie());
          continue;
        }
        if (statusCode < 200 || statusCode >= 300) {
          throw new IllegalStateException("接口调用失败，HTTP " + statusCode + "：" + truncate(responseBody, 500));
        }
        return objectMapper.readValue(responseBody, responseType);
      } catch (IOException e) {
        throw new IllegalStateException("接口调用失败：" + e.getMessage(), e);
      } finally {
        if (connection != null) {
          connection.disconnect();
        }
      }
    }
    throw new IllegalStateException("接口调用失败");
  }

  private HttpURLConnection openConnection(String url, String method) throws IOException {
    HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
    if (connection instanceof HttpsURLConnection httpsConnection) {
      configureHttps(httpsConnection);
    }
    connection.setRequestMethod(method);
    connection.setConnectTimeout(Math.max(properties.getConnectTimeoutMs(), 1));
    connection.setReadTimeout(Math.max(properties.getReadTimeoutMs(), 1));
    if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
      connection.setDoOutput(true);
    }
    return connection;
  }

  private void configureHttps(HttpsURLConnection connection) {
    if (!properties.isTrustAllSsl()) {
      return;
    }
    connection.setSSLSocketFactory(trustAllSocketFactory());
    connection.setHostnameVerifier(TRUST_ALL_HOSTNAME_VERIFIER);
  }

  private SSLSocketFactory trustAllSocketFactory() {
    SSLSocketFactory current = trustAllSocketFactory;
    if (current != null) {
      return current;
    }
    synchronized (PlatformIntegrationService.class) {
      if (trustAllSocketFactory == null) {
        try {
          TrustManager[] trustManagers = new TrustManager[] {
              new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                  return new X509Certificate[0];
                }
              }
          };
          SSLContext context = SSLContext.getInstance("TLS");
          context.init(null, trustManagers, new SecureRandom());
          trustAllSocketFactory = context.getSocketFactory();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
          throw new IllegalStateException("初始化HTTPS证书信任配置失败：" + e.getMessage(), e);
        }
      }
      return trustAllSocketFactory;
    }
  }

  private boolean isUnauthorized(int statusCode) {
    return statusCode == HttpURLConnection.HTTP_UNAUTHORIZED || statusCode == HttpURLConnection.HTTP_FORBIDDEN;
  }

  private void writeJson(HttpURLConnection connection, Object body) throws IOException {
    byte[] payload = objectMapper.writeValueAsBytes(body);
    connection.setFixedLengthStreamingMode(payload.length);
    try (var outputStream = connection.getOutputStream()) {
      outputStream.write(payload);
    }
  }

  private String modelChatUrl(String modelId) {
    String encodedModelId = URLEncoder.encode(modelId, StandardCharsets.UTF_8);
    String template = properties.getModelChatUrl();
    String replaced = template
        .replace("{modelId}", encodedModelId)
        .replace("{modelid}", encodedModelId);
    if (!replaced.equals(template)) {
      return replaced;
    }
    return template.endsWith("/") ? template + encodedModelId : template + "/" + encodedModelId;
  }

  private String agentDetailUrl(String agentId) {
    String encodedAgentId = URLEncoder.encode(agentId, StandardCharsets.UTF_8);
    String template = properties.getAgentDetailUrl();
    String replaced = template
        .replace("{agentId}", encodedAgentId)
        .replace("{agentid}", encodedAgentId);
    if (!replaced.equals(template)) {
      return replaced;
    }
    return template.endsWith("/") ? template + encodedAgentId : template + "/" + encodedAgentId;
  }

  private String agentChatUrl(String accessUrl) {
    String safeAccessUrl = requireText(accessUrl, "Agent detail response is missing accessUrl");
    String normalized = safeAccessUrl.endsWith("/")
        ? safeAccessUrl.substring(0, safeAccessUrl.length() - 1)
        : safeAccessUrl;
    return normalized + "/chat/completions";
  }

  private void ensureSuccess(String name, String status, Boolean success) {
    if (!"200".equals(status) || Boolean.FALSE.equals(success)) {
      throw new IllegalStateException(name + "返回失败，status=" + status + "，success=" + success);
    }
  }

  private PlatformAgentChatResponse parseAgentStream(
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
    Map<String, String> outputs = buildAgentOutputs(aggregate.choices);
    String rawOutput = firstNonBlank(outputs.get("rawText"), String.join("\n", aggregate.rawPayloads));
    String error = outputs.getOrDefault("error", "");
    return new PlatformAgentChatResponse(
        firstNonBlank(aggregate.id, UUID.randomUUID().toString().replace("-", "")),
        firstNonBlank(aggregate.conversationId, fallbackConversationId),
        aggregate.masterAgent,
        aggregate.metaAgent,
        firstNonBlank(aggregate.userId, ""),
        firstNonBlank(aggregate.object, RESPONSE_OBJECT),
        aggregate.created == null ? System.currentTimeMillis() : aggregate.created,
        firstNonBlank(aggregate.model, ""),
        aggregate.choices,
        StringUtils.hasText(error) ? STATUS_FAILED : STATUS_COMPLETED,
        outputs,
        System.currentTimeMillis() - startedAt,
        error,
        rawOutput);
  }

  private PlatformAgentChatResponse agentFailure(String agentAlias, String conversationId, long startedAt, String errorMessage) {
    List<PlatformAgentChoice> choices = List.of(choice(0, errorBlock(errorMessage)));
    Map<String, String> outputs = buildAgentOutputs(choices);
    return new PlatformAgentChatResponse(
        UUID.randomUUID().toString().replace("-", ""),
        conversationId,
        agentAlias,
        null,
        "",
        RESPONSE_OBJECT,
        System.currentTimeMillis(),
        "",
        choices,
        STATUS_FAILED,
        outputs,
        System.currentTimeMillis() - startedAt,
        errorMessage,
        errorMessage);
  }

  private void mergeAgentPayload(RemoteAgentAggregate aggregate, String payload) throws IOException {
    JsonNode root = objectMapper.readTree(payload);
    aggregate.id = firstNonBlank(textValue(root, "id"), aggregate.id);
    aggregate.conversationId = firstNonBlank(textValue(root, "conversationId"), aggregate.conversationId);
    aggregate.masterAgent = objectValue(root, "masterAgent", aggregate.masterAgent);
    aggregate.metaAgent = objectValue(root, "metaAgent", aggregate.metaAgent);
    aggregate.userId = firstNonBlank(textValue(root, "userId"), aggregate.userId);
    aggregate.object = firstNonBlank(textValue(root, "object"), aggregate.object);
    aggregate.model = firstNonBlank(textValue(root, "model"), aggregate.model);
    if (root.hasNonNull("created")) {
      aggregate.created = root.get("created").asLong();
    }
    JsonNode choicesNode = root.get("choices");
    if (choicesNode != null && choicesNode.isArray()) {
      for (JsonNode choiceNode : choicesNode) {
        aggregate.choices.add(parseChoice(choiceNode, aggregate.choices.size()));
      }
    }
  }

  private PlatformAgentChoice parseChoice(JsonNode choiceNode, int fallbackIndex) {
    Integer index = choiceNode != null && choiceNode.hasNonNull("index") ? choiceNode.get("index").asInt() : fallbackIndex;
    String finishReason = textValue(choiceNode, "finish_reason");
    JsonNode deltaNode = choiceNode == null ? null : choiceNode.get("delta");
    String role = firstNonBlank(textValue(deltaNode, "role"), ROLE_ASSISTANT);
    List<PlatformAgentContentBlock> contents = parseDeltaContents(deltaNode == null ? null : deltaNode.get("content"));
    List<PlatformAgentToolCallDelta> toolCalls = null;
    JsonNode toolCallsNode = deltaNode == null ? null : deltaNode.get("tool_calls");
    if (toolCallsNode != null && !toolCallsNode.isNull()) {
      toolCalls = objectMapper.convertValue(toolCallsNode, TOOL_CALLS_TYPE);
    }
    Map<String, Object> extra = null;
    JsonNode extraNode = deltaNode == null ? null : deltaNode.get("extra");
    if (extraNode != null && !extraNode.isNull()) {
      extra = objectMapper.convertValue(extraNode, EXTRA_TYPE);
    }
    return new PlatformAgentChoice(index, new PlatformAgentDelta(role, contents, toolCalls, extra), finishReason);
  }

  private List<PlatformAgentContentBlock> parseDeltaContents(JsonNode contentNode) {
    if (contentNode == null || contentNode.isNull()) {
      return List.of();
    }
    List<PlatformAgentContentBlock> contents = new ArrayList<>();
    if (contentNode.isArray()) {
      for (JsonNode item : contentNode) {
        contents.add(parseDeltaContent(item));
      }
    } else {
      contents.add(parseDeltaContent(contentNode));
    }
    return contents;
  }

  private PlatformAgentContentBlock parseDeltaContent(JsonNode item) {
    if (item == null || item.isNull()) {
      return new PlatformAgentContentBlock("text", "", null, null);
    }
    if (item.isTextual()) {
      return new PlatformAgentContentBlock("text", item.asText(), null, null);
    }
    String type = firstNonBlank(textValue(item, "type"), "text");
    String text = textValue(item, "text");
    String reasoning = textValue(item, "reasoning");
    String error = textValue(item, "error");
    String skillName = textValue(item, "skillName");
    String skillDesc = textValue(item, "skillDesc");
    String toolCallId = textValue(item, "toolCallId");
    String toolName = textValue(item, "toolName");
    String arguments = textValue(item, "arguments");
    String response = textValue(item, "response");
    List<PlatformAgentReferenceItem> references = parseReferences(item.get("references"));
    PlatformAgentUiCardDefinition uiCardDefinition = parseUiCardDefinition(item);
    Map<String, Object> extra = objectMapper.convertValue(item, EXTRA_TYPE);
    String normalizedType = type.trim();
    String fallbackValue = firstNonEmpty(text, reasoning, error, firstNonTypeFieldValue(item));
    if ("reasoning".equals(normalizedType)) {
      return new PlatformAgentContentBlock(normalizedType, null, firstNonEmpty(reasoning, text, fallbackValue), null,
          null, null, null, null, null, null, null, null, extra);
    }
    if ("error".equals(normalizedType)) {
      return new PlatformAgentContentBlock(normalizedType, null, null, firstNonEmpty(error, text, fallbackValue),
          null, null, null, null, null, null, null, null, extra);
    }
    if ("skill_trigger".equals(normalizedType)) {
      return new PlatformAgentContentBlock(normalizedType, null, null, null,
          skillName, skillDesc, null, null, null, null, null, null, extra);
    }
    if ("references".equals(normalizedType)) {
      return new PlatformAgentContentBlock(normalizedType, null, null, null,
          null, null, references, null, null, null, null, null, extra);
    }
    if ("tool_call".equals(normalizedType)) {
      return new PlatformAgentContentBlock(normalizedType, null, null, null,
          null, null, null, toolCallId, toolName, arguments, null, null, extra);
    }
    if ("tool_response".equals(normalizedType)) {
      return new PlatformAgentContentBlock(normalizedType, null, null, null,
          null, null, null, toolCallId, toolName, null, response, null, extra);
    }
    if ("gen_ui".equals(normalizedType)) {
      return new PlatformAgentContentBlock(normalizedType, null, null, null,
          null, null, null, null, null, null, null, uiCardDefinition, extra);
    }
    return new PlatformAgentContentBlock(normalizedType, firstNonEmpty(text, fallbackValue), null, null,
        null, null, null, null, null, null, null, null, extra);
  }

  private List<PlatformAgentReferenceItem> parseReferences(JsonNode referencesNode) {
    if (referencesNode == null || referencesNode.isNull()) {
      return List.of();
    }
    if (!referencesNode.isArray()) {
      return List.of(objectMapper.convertValue(referencesNode, PlatformAgentReferenceItem.class));
    }
    return objectMapper.convertValue(referencesNode, REFERENCES_TYPE);
  }

  private PlatformAgentUiCardDefinition parseUiCardDefinition(JsonNode item) {
    JsonNode uiCardNode = item == null ? null : item.get("uicardDefinition");
    if (uiCardNode == null || uiCardNode.isNull()) {
      uiCardNode = item == null ? null : item.get("uiCardDefinition");
    }
    if (uiCardNode == null || uiCardNode.isNull()) {
      return null;
    }
    return objectMapper.convertValue(uiCardNode, PlatformAgentUiCardDefinition.class);
  }

  private Map<String, String> buildAgentOutputs(List<PlatformAgentChoice> choices) {
    List<String> debugParts = new ArrayList<>();
    List<String> reasoningParts = new ArrayList<>();
    List<String> textParts = new ArrayList<>();
    List<String> errorParts = new ArrayList<>();
    List<String> skillTriggerParts = new ArrayList<>();
    List<String> referenceParts = new ArrayList<>();
    List<String> toolCallParts = new ArrayList<>();
    List<String> toolResponseParts = new ArrayList<>();
    List<String> genUiParts = new ArrayList<>();
    for (PlatformAgentChoice choice : choices) {
      if (choice == null || choice.delta() == null || choice.delta().content() == null) {
        continue;
      }
      for (PlatformAgentContentBlock content : choice.delta().content()) {
        appendContentPart(
            debugParts,
            reasoningParts,
            textParts,
            errorParts,
            skillTriggerParts,
            referenceParts,
            toolCallParts,
            toolResponseParts,
            genUiParts,
            content);
      }
    }
    Map<String, String> outputs = new LinkedHashMap<>();
    putIfText(outputs, "debug", joinStreamParts(debugParts));
    putIfText(outputs, "reasoning", joinStreamParts(reasoningParts));
    putIfText(outputs, "text", joinStreamParts(textParts));
    putIfText(outputs, "error", joinStreamParts(errorParts));
    putIfText(outputs, "skillTrigger", joinNonBlank("\n", skillTriggerParts.toArray(String[]::new)));
    putIfText(outputs, "references", joinNonBlank("\n", referenceParts.toArray(String[]::new)));
    putIfText(outputs, "toolCall", joinNonBlank("\n", toolCallParts.toArray(String[]::new)));
    putIfText(outputs, "toolResponse", joinNonBlank("\n", toolResponseParts.toArray(String[]::new)));
    putIfText(outputs, "genUi", joinNonBlank("\n", genUiParts.toArray(String[]::new)));
    putIfText(outputs, "answer", firstNonBlank(outputs.get("text")));
    putIfText(outputs, "content", firstNonBlank(outputs.get("text")));
    putIfText(outputs, "rawText", joinNonBlank(
        "\n",
        outputs.get("debug"),
        outputs.get("reasoning"),
        outputs.get("text"),
        outputs.get("skillTrigger"),
        outputs.get("references"),
        outputs.get("toolCall"),
        outputs.get("toolResponse"),
        outputs.get("genUi"),
        outputs.get("error")));
    return outputs;
  }

  private void appendContentPart(
      List<String> debugParts,
      List<String> reasoningParts,
      List<String> textParts,
      List<String> errorParts,
      List<String> skillTriggerParts,
      List<String> referenceParts,
      List<String> toolCallParts,
      List<String> toolResponseParts,
      List<String> genUiParts,
      PlatformAgentContentBlock content
  ) {
    if (content == null || !StringUtils.hasText(content.type())) {
      return;
    }
    String type = content.type().trim();
    String value = contentDisplayValue(content);
    if (value.isEmpty()) {
      return;
    }
    if ("debug".equals(type)) {
      debugParts.add(value);
    } else if ("reasoning".equals(type)) {
      reasoningParts.add(value);
    } else if ("text".equals(type)) {
      textParts.add(value);
    } else if ("error".equals(type)) {
      errorParts.add(value);
    } else if ("skill_trigger".equals(type)) {
      skillTriggerParts.add(value);
    } else if ("references".equals(type)) {
      referenceParts.add(value);
    } else if ("tool_call".equals(type)) {
      toolCallParts.add(value);
    } else if ("tool_response".equals(type)) {
      toolResponseParts.add(value);
    } else if ("gen_ui".equals(type)) {
      genUiParts.add(value);
    } else {
      textParts.add(value);
    }
  }

  private String contentDisplayValue(PlatformAgentContentBlock content) {
    String type = content.type().trim();
    if ("skill_trigger".equals(type)) {
      return joinNonBlank(" - ", content.skillName(), content.skillDesc());
    }
    if ("references".equals(type)) {
      return toJson(content.references());
    }
    if ("tool_call".equals(type)) {
      return toJson(Map.of(
          "toolCallId", firstNonBlank(content.toolCallId()),
          "toolName", firstNonBlank(content.toolName()),
          "arguments", firstNonBlank(content.arguments())));
    }
    if ("tool_response".equals(type)) {
      return toJson(Map.of(
          "toolCallId", firstNonBlank(content.toolCallId()),
          "toolName", firstNonBlank(content.toolName()),
          "response", firstNonBlank(content.response())));
    }
    if ("gen_ui".equals(type)) {
      return toJson(content.uiCardDefinition());
    }
    return firstNonEmpty(content.text(), content.reasoning(), content.error(), toJson(content.extra()));
  }

  private String toJson(Object value) {
    if (value == null) {
      return "";
    }
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception e) {
      return String.valueOf(value);
    }
  }

  private PlatformAgentChoice choice(Integer index, PlatformAgentContentBlock content) {
    return new PlatformAgentChoice(
        index,
        new PlatformAgentDelta(ROLE_ASSISTANT, content == null ? List.of() : List.of(content), null, null),
        null);
  }

  private PlatformAgentContentBlock contentBlock(String type, String text) {
    return new PlatformAgentContentBlock(type, text, null, null);
  }

  private PlatformAgentContentBlock errorBlock(String error) {
    return new PlatformAgentContentBlock("error", null, null, error);
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

  private Object objectValue(JsonNode node, String fieldName, Object fallback) {
    if (node == null || !node.has(fieldName) || node.get(fieldName).isNull()) {
      return fallback;
    }
    JsonNode value = node.get(fieldName);
    if (value.isTextual()) {
      return value.asText();
    }
    if (value.isNumber() || value.isBoolean()) {
      return value.asText();
    }
    return objectMapper.convertValue(value, Object.class);
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
      if (!"type".equals(entry.getKey())) {
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

  private static class RemoteAgentAggregate {
    private String id = "";
    private String conversationId;
    private Object masterAgent;
    private Object metaAgent;
    private String userId = "";
    private String object = "";
    private Long created;
    private String model = "";
    private final List<PlatformAgentChoice> choices = new ArrayList<>();
    private final List<String> rawPayloads = new ArrayList<>();

    private RemoteAgentAggregate(String conversationId) {
      this.conversationId = conversationId;
    }
  }
}
