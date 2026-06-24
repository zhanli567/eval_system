package com.evalsystem.integration.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.evalsystem.integration.config.PlatformIntegrationProperties;
import com.evalsystem.integration.api.dto.request.PlatformAgentChatRequest;
import com.evalsystem.integration.api.dto.request.PlatformAgentMessage;
import com.evalsystem.integration.api.dto.response.PlatformAgentDefinition;
import com.evalsystem.integration.api.dto.response.PlatformAgentVersion;
import com.evalsystem.integration.api.dto.response.PlatformModelInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class PlatformIntegrationServiceTest {
  private HttpServer server;

  @AfterEach
  void tearDown() {
    if (server != null) {
      server.stop(0);
    }
  }

  @Test
  void listModelsReloginsAndRetriesWhenCookieIsRejected() throws Exception {
    AtomicInteger loginCalls = new AtomicInteger();
    AtomicInteger modelCalls = new AtomicInteger();
    server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext("/login", exchange -> {
      int call = loginCalls.incrementAndGet();
      exchange.getResponseHeaders().add("Set-Cookie", "SESSION=" + (call == 1 ? "old" : "new"));
      writeJson(exchange, 200, "{\"statusCode\":0,\"statusText\":\"ok\"}");
    });
    server.createContext("/models", exchange -> {
      modelCalls.incrementAndGet();
      String cookie = firstHeader(exchange, "cookie");
      if (!cookie.contains("SESSION=new")) {
        writeJson(exchange, 403, "{\"message\":\"forbidden\"}");
        return;
      }
      writeJson(exchange, 200, """
          {
            "status": "200",
            "url": "/models",
            "success": true,
            "resultObjVO": {
              "list": [
                {
                  "modelId": "model-1",
                  "name": "模型一",
                  "timeoutPolicy": "default",
                  "capabilities": ["chat"]
                }
              ],
              "total": 1,
              "pageNum": 1,
              "pageSize": 10,
              "pages": 1
            }
          }
          """);
    });
    server.start();

    PlatformIntegrationService service = new PlatformIntegrationService(properties(), new ObjectMapper());

    List<PlatformModelInfo> models = service.listModels();

    assertThat(models).extracting(PlatformModelInfo::modelId).containsExactly("model-1");
    assertThat(loginCalls).hasValue(2);
    assertThat(modelCalls).hasValue(2);
  }

  @Test
  void getAgentDetailSendsAuthHeadersAndNormalizesChildren() throws Exception {
    AtomicReference<String> cookie = new AtomicReference<>("");
    AtomicReference<String> spaceId = new AtomicReference<>("");
    server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext("/login", exchange -> {
      exchange.getResponseHeaders().add("Set-Cookie", "SESSION=valid");
      writeJson(exchange, 200, "{\"statusCode\":0,\"statusText\":\"ok\"}");
    });
    server.createContext("/agents/agent-1", exchange -> {
      cookie.set(firstHeader(exchange, "cookie"));
      spaceId.set(firstHeader(exchange, "x-space-id"));
      writeJson(exchange, 200, """
          {
            "status": "200",
            "url": "/agents/agent-1",
            "success": true,
            "resultObjVO": {
              "superAgentId": "agent-1",
              "name": "agent-one",
              "displayName": "Agent One",
              "description": "Demo agent",
              "accessUrl": "http://localhost/dynamic",
              "currentBundleId": "bundle-current-id",
              "bundleVersion": "bundle-main",
              "instances": [
                {"instanceId": "i-1", "bundleVersion": "bundle-main"},
                {"instanceId": "i-2", "bundleVersion": ""},
                {"instanceId": "i-3", "bundleVersion": "bundle-old"},
                {"instanceId": "i-4", "bundleVersion": "bundle-old"}
              ],
              "loadedAgents": [
                {"agentAlias": "child-a", "metaAgentName": "Child A", "version": "1"},
                {"agentAlias": "", "metaAgentName": "Blank"},
                {"agentAlias": "child-a", "metaAgentName": "Child A Duplicate"},
                {"agentAlias": "child-b", "metaAgentName": "Child B", "version": "2"}
              ]
            }
          }
          """);
    });
    server.start();

    PlatformIntegrationService service = new PlatformIntegrationService(properties(), new ObjectMapper());

    PlatformAgentDefinition detail = service.getAgentDetail("agent-1");

    assertThat(cookie).hasValue("SESSION=valid");
    assertThat(spaceId).hasValue("space-1");
    assertThat(detail.id()).isEqualTo("agent-1");
    assertThat(detail.agentName()).isEqualTo("Agent One");
    assertThat(detail.versions()).isEmpty();
    assertThat(detail.childAgents()).extracting(child -> child.agentAlias()).containsExactly("child-a", "child-b");
    assertThat(detail.outputs()).extracting(output -> output.fieldName()).contains(
        "text",
        "reasoning",
        "debug",
        "error",
        "rawText",
        "skillTrigger",
        "references",
        "toolCall",
        "toolResponse",
        "genUi");
  }

  @Test
  void listAgentBundlesSendsAuthHeadersAndNormalizesItems() throws Exception {
    AtomicReference<String> cookie = new AtomicReference<>("");
    AtomicReference<String> spaceId = new AtomicReference<>("");
    server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext("/login", exchange -> {
      exchange.getResponseHeaders().add("Set-Cookie", "SESSION=valid");
      writeJson(exchange, 200, "{\"statusCode\":0,\"statusText\":\"ok\"}");
    });
    server.createContext("/agents/agent-1/bundles", exchange -> {
      cookie.set(firstHeader(exchange, "cookie"));
      spaceId.set(firstHeader(exchange, "x-space-id"));
      writeJson(exchange, 200, """
          {
            "status": "200",
            "url": "/agents/agent-1/bundles",
            "resultObjVO": {
              "superAgentId": "agent-1",
              "currentBundleId": "bundle-current",
              "items": [
                {"bundleId": "bundle-current", "status": "online", "bundleVersion": "v1"},
                {"bundleId": "", "status": "online", "bundleVersion": "empty-id"},
                {"bundleId": "bundle-old", "status": "offline", "bundleVersion": ""},
                {"bundleId": "bundle-old", "status": "offline", "bundleVersion": "duplicate"}
              ]
            },
            "success": true
          }
          """);
    });
    server.start();

    PlatformIntegrationService service = new PlatformIntegrationService(properties(), new ObjectMapper());

    List<PlatformAgentVersion> bundles = service.listAgentBundles("agent-1");

    assertThat(cookie).hasValue("SESSION=valid");
    assertThat(spaceId).hasValue("space-1");
    assertThat(bundles).extracting(PlatformAgentVersion::id).containsExactly("bundle-current", "bundle-old");
    assertThat(bundles).extracting(PlatformAgentVersion::versionName).containsExactly("v1", "bundle-old");
  }

  @Test
  void invokeAgentSendsMessagesArrayField() throws Exception {
    AtomicReference<String> requestBody = new AtomicReference<>("");
    ObjectMapper objectMapper = new ObjectMapper();
    server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext("/login", exchange -> {
      exchange.getResponseHeaders().add("Set-Cookie", "SESSION=valid");
      writeJson(exchange, 200, "{\"statusCode\":0,\"statusText\":\"ok\"}");
    });
    server.createContext("/agent/chat", exchange -> {
      requestBody.set(readBody(exchange));
      writeJson(exchange, 200, """
          {"choices":[{"delta":{"content":[{"type":"text","text":"ok"}]}}]}
          """);
    });
    server.start();

    PlatformIntegrationService service = new PlatformIntegrationService(properties(), objectMapper);
    service.invokeAgent(
        "router-agent",
        "bundle-1",
        "",
        new PlatformAgentChatRequest("conversation-1", List.of(new PlatformAgentMessage("user", "hello")), true));

    JsonNode root = objectMapper.readTree(requestBody.get());
    assertThat(root.has("messages")).isTrue();
    assertThat(root.has("message")).isFalse();
    assertThat(root.get("messages").get(0).get("role").asText()).isEqualTo("user");
    assertThat(root.get("messages").get(0).get("content").asText()).isEqualTo("hello");
  }

  @Test
  void invokeAgentUsesUnifiedChatUrlAndSendsAgentAndBundleHeaders() throws Exception {
    AtomicReference<String> requestBody = new AtomicReference<>("");
    AtomicReference<String> childAlias = new AtomicReference<>("missing");
    AtomicReference<String> superAgentId = new AtomicReference<>("");
    AtomicReference<String> bundleId = new AtomicReference<>("");
    ObjectMapper objectMapper = new ObjectMapper();
    server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext("/login", exchange -> {
      exchange.getResponseHeaders().add("Set-Cookie", "SESSION=valid");
      writeJson(exchange, 200, "{\"statusCode\":0,\"statusText\":\"ok\"}");
    });
    server.createContext("/agent/chat", exchange -> {
      requestBody.set(readBody(exchange));
      childAlias.set(firstHeader(exchange, "x-agent-alias"));
      superAgentId.set(firstHeader(exchange, "x-super-agent-id"));
      bundleId.set(firstHeader(exchange, "x-bundle-id"));
      writeJson(exchange, 200, """
          {"choices":[{"delta":{"content":[{"type":"text","text":"dynamic ok"}]}}]}
          """);
    });
    server.start();

    PlatformIntegrationService service = new PlatformIntegrationService(properties(), objectMapper);
    var response = service.invokeAgent(
        "agent-1",
        "bundle-1",
        "",
        new PlatformAgentChatRequest("conversation-1", List.of(new PlatformAgentMessage("user", "hello")), true));

    JsonNode root = objectMapper.readTree(requestBody.get());
    assertThat(root.get("messages").get(0).get("content").asText()).isEqualTo("hello");
    assertThat(childAlias).hasValue("");
    assertThat(superAgentId).hasValue("agent-1");
    assertThat(bundleId).hasValue("bundle-1");
    assertThat(response.outputs().get("text")).isEqualTo("dynamic ok");
  }

  @Test
  void invokeAgentSendsSelectedChildAliasHeader() throws Exception {
    AtomicReference<String> childAlias = new AtomicReference<>("");
    server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext("/login", exchange -> {
      exchange.getResponseHeaders().add("Set-Cookie", "SESSION=valid");
      writeJson(exchange, 200, "{\"statusCode\":0,\"statusText\":\"ok\"}");
    });
    server.createContext("/agent/chat", exchange -> {
      childAlias.set(firstHeader(exchange, "x-agent-alias"));
      writeJson(exchange, 200, """
          {"choices":[{"delta":{"content":[{"type":"text","text":"child ok"}]}}]}
          """);
    });
    server.start();

    PlatformIntegrationService service = new PlatformIntegrationService(properties(), new ObjectMapper());
    service.invokeAgent(
        "agent-1",
        "bundle-1",
        "child-a",
        new PlatformAgentChatRequest("conversation-1", List.of(new PlatformAgentMessage("user", "hello")), true));

    assertThat(childAlias).hasValue("child-a");
  }

  @Test
  void invokeAgentConcatenatesStreamingTextChunks() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext("/login", exchange -> {
      exchange.getResponseHeaders().add("Set-Cookie", "SESSION=valid");
      writeJson(exchange, 200, "{\"statusCode\":0,\"statusText\":\"ok\"}");
    });
    server.createContext("/agent/chat", exchange -> {
      byte[] payload = """
          data: {"choices":[{"delta":{"content":[{"type":"text","text":"Hello"}]}}]}
          data: {"choices":[{"delta":{"content":[{"type":"text","text":" "}]}}]}
          data: {"choices":[{"delta":{"content":[{"type":"text","text":"world"}]}}]}
          data: [DONE]
          """.getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().set("Content-Type", "text/event-stream;charset=UTF-8");
      exchange.sendResponseHeaders(200, payload.length);
      try (var output = exchange.getResponseBody()) {
        output.write(payload);
      }
    });
    server.start();

    PlatformIntegrationService service = new PlatformIntegrationService(properties(), objectMapper);
    var response = service.invokeAgent(
        "router-agent",
        "bundle-1",
        "",
        new PlatformAgentChatRequest("conversation-1", List.of(new PlatformAgentMessage("user", "hello")), true));

    assertThat(response.outputs().get("text")).isEqualTo("Hello world");
  }

  @Test
  void invokeAgentParsesAllStructuredContentTypes() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext("/login", exchange -> {
      exchange.getResponseHeaders().add("Set-Cookie", "SESSION=valid");
      writeJson(exchange, 200, "{\"statusCode\":0,\"statusText\":\"ok\"}");
    });
    server.createContext("/agent/chat", exchange -> {
      byte[] payload = """
          data: {"id":"chunk-1","conversationId":"conversation-1","masterAgent":{"name":"master"},"metaAgent":{"name":"meta"},"userId":"user-1","object":"chat.completion.chunk","created":1781573919962,"model":"agent-model","choices":[{"index":0,"delta":{"role":"assistant","content":[{"type":"reasoning","reasoning":"thinking"},{"type":"skill_trigger","skillName":"search","skillDesc":"search docs"},{"type":"references","references":[{"id":"ref-1","title":"Doc title","url":"https://example.com/doc","sourceType":"web","sourceName":"Example","summary":"short summary","snippet":"matched snippet"}]},{"type":"debug","text":"debug line"},{"type":"text","text":"final answer"},{"type":"tool_call","toolCallId":"call-1","toolName":"lookup","arguments":"{\\"q\\":\\"abc\\"}"},{"type":"tool_response","toolCallId":"call-1","toolName":"lookup","response":"tool result"},{"type":"gen_ui","uicardDefinition":{"id":"card-1","type":"chart","version":"1","displayName":"Chart","location":"CHAT_UI","body":[{"id":"component-1","type":"text","componentKey":"Text","propsData":{"value":"hello"}}]}},{"type":"error","error":"minor error"}],"tool_calls":[{"index":0,"id":"call-2","type":"function","function":{"name":"fn","arguments":"{\\"x\\":1}"}}],"extra":{"traceId":"trace-1"}},"finish_reason":"stop"}]}
          data: [DONE]
          """.getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().set("Content-Type", "text/event-stream;charset=UTF-8");
      exchange.sendResponseHeaders(200, payload.length);
      try (var output = exchange.getResponseBody()) {
        output.write(payload);
      }
    });
    server.start();

    PlatformIntegrationService service = new PlatformIntegrationService(properties(), objectMapper);

    var response = service.invokeAgent(
        "router-agent",
        "bundle-1",
        "",
        new PlatformAgentChatRequest("conversation-1", List.of(new PlatformAgentMessage("user", "hello")), true));

    var delta = response.choices().getFirst().delta();
    assertThat(delta.toolCalls().getFirst().function().name()).isEqualTo("fn");
    assertThat(delta.extra()).containsEntry("traceId", "trace-1");
    assertThat(delta.content()).extracting(content -> content.type()).containsExactly(
        "reasoning",
        "skill_trigger",
        "references",
        "debug",
        "text",
        "tool_call",
        "tool_response",
        "gen_ui",
        "error");
    assertThat(delta.content().get(1).skillName()).isEqualTo("search");
    assertThat(delta.content().get(2).references().getFirst().sourceType().value()).isEqualTo("web");
    assertThat(delta.content().get(5).toolCallId()).isEqualTo("call-1");
    assertThat(delta.content().get(7).uiCardDefinition().body().getFirst().componentKey()).isEqualTo("Text");
    assertThat(response.outputs()).containsEntry("text", "final answer");
    assertThat(response.outputs().get("skillTrigger")).contains("search").contains("search docs");
    assertThat(response.outputs().get("references")).contains("Doc title").contains("https://example.com/doc");
    assertThat(response.outputs().get("toolCall")).contains("lookup").contains("call-1");
    assertThat(response.outputs().get("toolResponse")).contains("tool result");
    assertThat(response.outputs().get("genUi")).contains("card-1").contains("Chart");
    assertThat(response.outputs().get("rawText")).contains("thinking").contains("final answer").contains("minor error");
  }

  @Test
  void listModelsFiltersIamModelsWhenIamRouteIsEnabled() throws Exception {
    server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext("/login", exchange -> {
      exchange.getResponseHeaders().add("Set-Cookie", "SESSION=valid");
      writeJson(exchange, 200, "{\"statusCode\":0,\"statusText\":\"ok\"}");
    });
    server.createContext("/models", exchange -> writeJson(exchange, 200, """
        {
          "status": "200",
          "url": "/models",
          "success": true,
          "resultObjVO": {
            "list": [
              {
                "modelId": "iam-model",
                "name": "IAM Model",
                "modelName": "MES-Qwen-V35-35B-A3B",
                "authType": "IAM",
                "capabilities": ["chat"]
              },
              {
                "modelId": "gateway-model",
                "name": "Gateway Model",
                "modelName": "Gateway-Qwen",
                "authType": "COOKIE",
                "capabilities": ["chat"]
              }
            ],
            "total": 2,
            "pageNum": 1,
            "pageSize": 10,
            "pages": 1
          }
        }
        """));
    server.start();

    PlatformIntegrationProperties properties = properties();
    properties.getIam().setEnabled(true);
    PlatformIntegrationService service = new PlatformIntegrationService(properties, new ObjectMapper());

    List<PlatformModelInfo> models = service.listModels();

    assertThat(models).extracting(PlatformModelInfo::modelId).containsExactly("iam-model");
  }

  @Test
  void chatModelUsesIamEndpointAndCleansThinkContentWhenIamRouteIsEnabled() throws Exception {
    AtomicReference<String> authorization = new AtomicReference<>("");
    AtomicReference<String> requestBody = new AtomicReference<>("");
    ObjectMapper objectMapper = new ObjectMapper();
    server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext("/login", exchange -> {
      exchange.getResponseHeaders().add("Set-Cookie", "SESSION=valid");
      writeJson(exchange, 200, "{\"statusCode\":0,\"statusText\":\"ok\"}");
    });
    server.createContext("/models", exchange -> writeJson(exchange, 200, """
        {
          "status": "200",
          "url": "/models",
          "success": true,
          "resultObjVO": {
            "list": [
              {
                "modelId": "iam-model",
                "name": "IAM Model",
                "modelName": "MES-Qwen-V35-35B-A3B",
                "authType": "IAM",
                "capabilities": ["chat"]
              }
            ],
            "total": 1,
            "pageNum": 1,
            "pageSize": 10,
            "pages": 1
          }
        }
        """));
    server.createContext("/iam/chat", exchange -> {
      authorization.set(firstHeader(exchange, "authorization"));
      requestBody.set(readBody(exchange));
      writeJson(exchange, 200, """
          {
            "id": "chatcmpl-81f239c454",
            "object": "chat.completion.chunk",
            "created": 1781573919962,
            "model": "Qwen-V3.5-35B-A3B-8K",
            "choices": [
              {
                "index": 0,
                "message": {
                  "role": "assistant",
                  "content": "Thinking\\nprivate reasoning</think>\\n{\\"score\\":5,\\"reason\\":\\"ok\\"}"
                },
                "finish_reason": null,
                "logprobs": null
              }
            ],
            "usage": {
              "prompt_token": 11,
              "completion_tokens": 1022,
              "total_tokens": 1033
            },
            "message": ""
          }
          """);
    });
    server.start();

    PlatformIntegrationProperties properties = properties();
    properties.getIam().setEnabled(true);
    properties.getIam().setUrl("http://localhost:" + server.getAddress().getPort() + "/iam/chat");
    properties.getIam().setAuthorization("eyJhbGci");
    PlatformIntegrationService service = new PlatformIntegrationService(properties, objectMapper);

    var result = service.chatModel("iam-model", "please score");

    JsonNode root = objectMapper.readTree(requestBody.get());
    assertThat(authorization).hasValue("eyJhbGci");
    assertThat(root.get("model").asText()).isEqualTo("MES-Qwen-V35-35B-A3B");
    assertThat(root.get("stream").asBoolean()).isFalse();
    assertThat(root.get("messages").get(0).get("role").asText()).isEqualTo("user");
    assertThat(root.get("messages").get(0).get("content").asText()).isEqualTo("please score");
    assertThat(result.modelId()).isEqualTo("iam-model");
    assertThat(result.outputText()).isEqualTo("{\"score\":5,\"reason\":\"ok\"}");
  }

  private PlatformIntegrationProperties properties() {
    String baseUrl = "http://localhost:" + server.getAddress().getPort();
    PlatformIntegrationProperties properties = new PlatformIntegrationProperties();
    properties.setModelListUrl(baseUrl + "/models");
    properties.setAgentListUrl(baseUrl + "/agents");
    properties.setAgentDetailUrl(baseUrl + "/agents/{agentId}");
    properties.setAgentBundleListUrl(baseUrl + "/agents/{superAgentId}/bundles");
    properties.setModelChatUrl(baseUrl + "/chat/{modelId}");
    properties.setAgentChatUrl(baseUrl + "/agent/chat");
    properties.setXSpaceId("space-1");
    properties.getLogin().setUrl(baseUrl + "/login");
    properties.getLogin().setLoginAccount("account");
    properties.getLogin().setUid("uid");
    properties.getLogin().setPassword("password");
    properties.getLogin().setLang("zh_CN");
    properties.getLogin().setAppId("app");
    properties.getLogin().setEncryptedPasswordSwitch("false");
    return properties;
  }

  private void writeJson(HttpExchange exchange, int status, String body) throws IOException {
    byte[] payload = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "application/json;charset=UTF-8");
    exchange.sendResponseHeaders(status, payload.length);
    try (var output = exchange.getResponseBody()) {
      output.write(payload);
    }
  }

  private String firstHeader(HttpExchange exchange, String headerName) {
    return exchange.getRequestHeaders().getOrDefault(headerName, List.of("")).getFirst();
  }

  private String readBody(HttpExchange exchange) throws IOException {
    return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
  }
}
