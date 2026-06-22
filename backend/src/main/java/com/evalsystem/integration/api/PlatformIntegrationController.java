package com.evalsystem.integration.api;

import com.evalsystem.common.ApiResponse;
import com.evalsystem.integration.api.dto.response.PlatformAgentDefinition;
import com.evalsystem.integration.api.dto.request.PlatformModelChatRequest;
import com.evalsystem.integration.api.dto.response.PlatformModelChatResult;
import com.evalsystem.integration.api.dto.response.PlatformModelInfo;
import com.evalsystem.integration.service.PlatformIntegrationService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/integration")
public class PlatformIntegrationController {
  private final PlatformIntegrationService integrationService;

  public PlatformIntegrationController(PlatformIntegrationService integrationService) {
    this.integrationService = integrationService;
  }

  @GetMapping("/models")
  public ApiResponse<List<PlatformModelInfo>> listModels() {
    return ApiResponse.ok(integrationService.listModels());
  }

  @GetMapping("/agents")
  public ApiResponse<List<PlatformAgentDefinition>> listAgents() {
    return ApiResponse.ok(integrationService.listAgents());
  }

  @GetMapping("/agents/{agentId}")
  public ApiResponse<PlatformAgentDefinition> getAgentDetail(@PathVariable String agentId) {
    return ApiResponse.ok(integrationService.getAgentDetail(agentId));
  }

  @PostMapping("/models/{modelId}/chat")
  public ApiResponse<PlatformModelChatResult> chatModel(
      @PathVariable String modelId,
      @RequestBody PlatformModelChatRequest request
  ) {
    return ApiResponse.ok(integrationService.chatModel(modelId, request == null ? "" : request.message()));
  }
}
