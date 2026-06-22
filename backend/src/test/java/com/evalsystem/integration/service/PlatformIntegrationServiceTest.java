package com.evalsystem.integration.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.evalsystem.integration.config.PlatformIntegrationProperties;
import com.evalsystem.integration.api.dto.request.PlatformAgentChatRequest;
import com.evalsystem.integration.api.dto.request.PlatformAgentMessage;
import com.evalsystem.integration.api.dto.response.PlatformAgentDefinition;
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
      String cookie = firstHeader(exchange, "Cookie");
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
  void getAgentDetailSendsAuthHeadersAndNormalizesSnapshotsAndChildren() throws Exception {
    AtomicReference<String> cookie = new AtomicReference<>("");
    AtomicReference<String> spaceId = new AtomicReference<>("");
    server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext("/login", exchange -> {
      exchange.getResponseHeaders().add("Set-Cookie", "SESSION=valid");
      writeJson(exchange, 200, "{\"statusCode\":0,\"statusText\":\"ok\"}");
    });
    server.createContext("/agents/agent-1", exchange -> {
      cookie.set(firstHeader(exchange, "Cookie"));
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
    assertThat(detail.versions()).extracting(version -> version.id()).containsExactly("bundle-main", "bundle-old");
    assertThat(detail.childAgents()).extracting(child -> child.agentAlias()).containsExactly("child-a", "child-b");
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
    createAgentDetailContext("router-agent", "/dynamic");
    server.createContext("/dynamic/chat/completions", exchange -> {
      requestBody.set(readBody(exchange));
      writeJson(exchange, 200, """
          {"choices":[{"delta":{"content":[{"type":"text","text":"ok"}]}}]}
          """);
    });
    server.start();

    PlatformIntegrationService service = new PlatformIntegrationService(properties(), objectMapper);
    service.invokeAgent(
        "router-agent",
        new PlatformAgentChatRequest("conversation-1", List.of(new PlatformAgentMessage("user", "hello")), true));

    JsonNode root = objectMapper.readTree(requestBody.get());
    assertThat(root.has("messages")).isTrue();
    assertThat(root.has("message")).isFalse();
    assertThat(root.get("messages").get(0).get("role").asText()).isEqualTo("user");
    assertThat(root.get("messages").get(0).get("content").asText()).isEqualTo("hello");
  }

  @Test
  void invokeAgentUsesDynamicAccessUrlAndOmitsBlankChildAlias() throws Exception {
    AtomicReference<String> requestBody = new AtomicReference<>("");
    AtomicReference<String> childAlias = new AtomicReference<>("missing");
    ObjectMapper objectMapper = new ObjectMapper();
    server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext("/login", exchange -> {
      exchange.getResponseHeaders().add("Set-Cookie", "SESSION=valid");
      writeJson(exchange, 200, "{\"statusCode\":0,\"statusText\":\"ok\"}");
    });
    server.createContext("/agents/agent-1", exchange -> writeJson(exchange, 200, """
        {
          "status": "200",
          "url": "/agents/agent-1",
          "success": true,
          "resultObjVO": {
            "superAgentId": "agent-1",
            "displayName": "Agent One",
            "accessUrl": "http://localhost:%d/dynamic",
            "bundleVersion": "bundle-main"
          }
        }
        """.formatted(server.getAddress().getPort())));
    server.createContext("/dynamic/chat/completions", exchange -> {
      requestBody.set(readBody(exchange));
      childAlias.set(firstHeader(exchange, "x-agent-alias"));
      writeJson(exchange, 200, """
          {"choices":[{"delta":{"content":[{"type":"text","text":"dynamic ok"}]}}]}
          """);
    });
    server.start();

    PlatformIntegrationService service = new PlatformIntegrationService(properties(), objectMapper);
    var response = service.invokeAgent(
        "agent-1",
        "",
        new PlatformAgentChatRequest("conversation-1", List.of(new PlatformAgentMessage("user", "hello")), true));

    JsonNode root = objectMapper.readTree(requestBody.get());
    assertThat(root.get("messages").get(0).get("content").asText()).isEqualTo("hello");
    assertThat(childAlias).hasValue("");
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
    server.createContext("/agents/agent-1", exchange -> writeJson(exchange, 200, """
        {
          "status": "200",
          "url": "/agents/agent-1",
          "success": true,
          "resultObjVO": {
            "superAgentId": "agent-1",
            "displayName": "Agent One",
            "accessUrl": "http://localhost:%d/dynamic",
            "bundleVersion": "bundle-main"
          }
        }
        """.formatted(server.getAddress().getPort())));
    server.createContext("/dynamic/chat/completions", exchange -> {
      childAlias.set(firstHeader(exchange, "x-agent-alias"));
      writeJson(exchange, 200, """
          {"choices":[{"delta":{"content":[{"type":"text","text":"child ok"}]}}]}
          """);
    });
    server.start();

    PlatformIntegrationService service = new PlatformIntegrationService(properties(), new ObjectMapper());
    service.invokeAgent(
        "agent-1",
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
    createAgentDetailContext("router-agent", "/dynamic");
    server.createContext("/dynamic/chat/completions", exchange -> {
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
        new PlatformAgentChatRequest("conversation-1", List.of(new PlatformAgentMessage("user", "hello")), true));

    assertThat(response.outputs().get("text")).isEqualTo("Hello world");
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
      authorization.set(firstHeader(exchange, "Authorization"));
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
    properties.setModelChatUrl(baseUrl + "/chat/{modelId}");
    properties.setSuperAgentChatUrl(baseUrl + "/agent/chat");
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

  private void createAgentDetailContext(String agentId, String accessPath) {
    String baseUrl = "http://localhost:" + server.getAddress().getPort();
    server.createContext("/agents/" + agentId, exchange -> writeJson(exchange, 200, """
        {
          "status": "200",
          "url": "/agents/%s",
          "success": true,
          "resultObjVO": {
            "superAgentId": "%s",
            "displayName": "%s",
            "accessUrl": "%s%s",
            "bundleVersion": "bundle-main"
          }
        }
        """.formatted(agentId, agentId, agentId, baseUrl, accessPath)));
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
