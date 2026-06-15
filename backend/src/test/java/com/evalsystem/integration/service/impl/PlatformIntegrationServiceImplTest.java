package com.evalsystem.integration.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.evalsystem.integration.config.PlatformIntegrationProperties;
import com.evalsystem.integration.dto.PlatformAgentChatRequest;
import com.evalsystem.integration.dto.PlatformAgentMessage;
import com.evalsystem.integration.dto.PlatformModelInfo;
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

class PlatformIntegrationServiceImplTest {
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

    PlatformIntegrationServiceImpl service = new PlatformIntegrationServiceImpl(properties(), new ObjectMapper());

    List<PlatformModelInfo> models = service.listModels();

    assertThat(models).extracting(PlatformModelInfo::modelId).containsExactly("model-1");
    assertThat(loginCalls).hasValue(2);
    assertThat(modelCalls).hasValue(2);
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

    PlatformIntegrationServiceImpl service = new PlatformIntegrationServiceImpl(properties(), objectMapper);
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

    PlatformIntegrationServiceImpl service = new PlatformIntegrationServiceImpl(properties(), objectMapper);
    var response = service.invokeAgent(
        "router-agent",
        new PlatformAgentChatRequest("conversation-1", List.of(new PlatformAgentMessage("user", "hello")), true));

    assertThat(response.outputs().get("text")).isEqualTo("Hello world");
  }

  private PlatformIntegrationProperties properties() {
    String baseUrl = "http://localhost:" + server.getAddress().getPort();
    PlatformIntegrationProperties properties = new PlatformIntegrationProperties();
    properties.setModelListUrl(baseUrl + "/models");
    properties.setAgentListUrl(baseUrl + "/agents");
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
