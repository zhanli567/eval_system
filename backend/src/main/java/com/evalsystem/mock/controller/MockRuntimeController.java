package com.evalsystem.mock.controller;

import com.evalsystem.common.ApiResponse;
import com.evalsystem.mock.dto.MockAgentChatRequest;
import com.evalsystem.mock.dto.MockAgentChatResponse;
import com.evalsystem.mock.dto.MockAgentDefinition;
import com.evalsystem.mock.dto.MockEvaluatorRequest;
import com.evalsystem.mock.dto.MockEvaluatorResponse;
import com.evalsystem.mock.service.MockRuntimeService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mock")
public class MockRuntimeController {
  private final MockRuntimeService mockRuntimeService;

  public MockRuntimeController(MockRuntimeService mockRuntimeService) {
    this.mockRuntimeService = mockRuntimeService;
  }

  @GetMapping("/agents")
  public ApiResponse<List<MockAgentDefinition>> listAgents() {
    return ApiResponse.ok(mockRuntimeService.listAgents());
  }

  @PostMapping("/agent/chat")
  public ApiResponse<MockAgentChatResponse> invokeAgent(
      @RequestHeader(name = "x-agent-alias", defaultValue = "router-agent") String agentAlias,
      @RequestBody MockAgentChatRequest request
  ) {
    return ApiResponse.ok(mockRuntimeService.invokeAgent(agentAlias, request));
  }

  @PostMapping("/evaluators/evaluate")
  public ApiResponse<MockEvaluatorResponse> evaluateEvaluator(@RequestBody MockEvaluatorRequest request) {
    return ApiResponse.ok(mockRuntimeService.evaluateEvaluator(request));
  }
}
