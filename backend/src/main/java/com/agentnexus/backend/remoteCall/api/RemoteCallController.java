package com.agentnexus.backend.remoteCall.api;

import com.agentnexus.backend.common.ApiResponse;
import com.agentnexus.backend.remoteCall.api.dto.response.AgentDefinition;
import com.agentnexus.backend.remoteCall.api.dto.response.AgentVersion;
import com.agentnexus.backend.remoteCall.api.dto.response.ModelInfo;
import com.agentnexus.backend.remoteCall.api.dto.response.SpaceInfo;
import com.agentnexus.backend.remoteCall.service.RemoteCallService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ResponseBody;

@Component
@ResponseBody
@Path("/remoteCall")
public class RemoteCallController {
  private final RemoteCallService remoteCallService;

  public RemoteCallController(RemoteCallService remoteCallService) {
    this.remoteCallService = remoteCallService;
  }

  @GET
  @Path("/models")
  public ApiResponse<List<ModelInfo>> listModels() {
    return ApiResponse.ok(remoteCallService.listModels());
  }

  @GET
  @Path("/agents")
  public ApiResponse<List<AgentDefinition>> listAgents() {
    return ApiResponse.ok(remoteCallService.listAgents());
  }

  @GET
  @Path("/agents/{agentId}")
  public ApiResponse<AgentDefinition> getAgentDetail(@PathParam("agentId") String agentId) {
    return ApiResponse.ok(remoteCallService.getAgentDetail(agentId));
  }

  @GET
  @Path("/agents/{agentId}/bundles")
  public ApiResponse<List<AgentVersion>> listAgentBundles(@PathParam("agentId") String agentId) {
    return ApiResponse.ok(remoteCallService.listAgentBundles(agentId));
  }

  @GET
  @Path("/spaces/{pageSize}/{curPage}")
  public ApiResponse<List<SpaceInfo>> listSpaces(
      @PathParam("pageSize") int pageSize,
      @PathParam("curPage") int curPage,
      @HeaderParam("Cookie") String cookie
  ) {
    return ApiResponse.ok(remoteCallService.listSpaces(pageSize, curPage, cookie));
  }

}
