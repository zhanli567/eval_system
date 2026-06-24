package com.evalsystem.integration.api;

import com.evalsystem.common.ApiResponse;
import com.evalsystem.integration.api.dto.response.PlatformAgentDefinition;
import com.evalsystem.integration.api.dto.response.PlatformAgentVersion;
import com.evalsystem.integration.api.dto.request.PlatformModelChatRequest;
import com.evalsystem.integration.api.dto.response.PlatformModelChatResult;
import com.evalsystem.integration.api.dto.response.PlatformModelInfo;
import com.evalsystem.integration.service.PlatformIntegrationService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ResponseBody;

@Component
@ResponseBody
@Path("/api/integration")
public class PlatformIntegrationController {
  private final PlatformIntegrationService integrationService;

  public PlatformIntegrationController(PlatformIntegrationService integrationService) {
    this.integrationService = integrationService;
  }

  @GET
  @Path("/models")
  public ApiResponse<List<PlatformModelInfo>> listModels() {
    return ApiResponse.ok(integrationService.listModels());
  }

  @GET
  @Path("/agents")
  public ApiResponse<List<PlatformAgentDefinition>> listAgents() {
    return ApiResponse.ok(integrationService.listAgents());
  }

  @GET
  @Path("/agents/{agentId}")
  public ApiResponse<PlatformAgentDefinition> getAgentDetail(@PathParam("agentId") String agentId) {
    return ApiResponse.ok(integrationService.getAgentDetail(agentId));
  }

  @GET
  @Path("/agents/{agentId}/bundles")
  public ApiResponse<List<PlatformAgentVersion>> listAgentBundles(@PathParam("agentId") String agentId) {
    return ApiResponse.ok(integrationService.listAgentBundles(agentId));
  }

  @POST
  @Path("/models/{modelId}/chat")
  public ApiResponse<PlatformModelChatResult> chatModel(
      @PathParam("modelId") String modelId,
      PlatformModelChatRequest request
  ) {
    return ApiResponse.ok(integrationService.chatModel(modelId, request == null ? "" : request.message()));
  }
}
