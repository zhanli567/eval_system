package com.agentnexus.backend.remoteCall.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.agentnexus.backend.iam.IamTokenService;
import com.agentnexus.backend.remoteCall.api.dto.request.AgentChatRequest;
import com.agentnexus.backend.remoteCall.api.dto.request.AgentMessage;
import com.agentnexus.backend.remoteCall.client.LocalMockRemoteCallServiceClient;
import com.agentnexus.backend.remoteCall.config.RemoteCallProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class RemoteCallServiceMockModeTest {
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void chatModelReturnsEvaluationJsonWhenLocalMockIsEnabled() throws Exception {
    RemoteCallService service = createMockService();

    var result = service.chatModel("local-mock-model", "local-mock-model", "input");
    var output = objectMapper.readTree(result.outputText());

    assertThat(result.modelId()).isEqualTo("local-mock-model");
    assertThat(output.get("score").asInt()).isEqualTo(100);
    assertThat(output.get("reason").asText()).isEqualTo("Local mock evaluation result");
  }

  @Test
  void invokeAgentCompletesWhenLocalMockIsEnabled() {
    RemoteCallService service = createMockService();

    var response = service.invokeAgent(
        "local-mock-agent",
        "local-mock-bundle-v1",
        "qa-agent",
        new AgentChatRequest(
            "conversation-1",
            List.of(new AgentMessage("user", "question: hello")),
            true));

    assertThat(response.status()).isEqualTo("completed");
    assertThat(response.outputs().get("text")).contains("question: hello");
    assertThat(response.outputs().get("rawText")).contains("question: hello");
  }

  private RemoteCallService createMockService() {
    RemoteCallProperties properties = new RemoteCallProperties();
    properties.getMock().setEnabled(true);
    return new RemoteCallService(
        properties,
        objectMapper,
        new LocalMockRemoteCallServiceClient(),
        new IamTokenService());
  }
}
