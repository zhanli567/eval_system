package com.agentnexus.backend.remoteCall.client;

import com.agentnexus.backend.remoteCall.api.dto.response.AgentBundleListResult;
import com.agentnexus.backend.remoteCall.api.dto.response.ListResult;
import com.agentnexus.backend.remoteCall.api.dto.response.ModelInfo;
import com.agentnexus.backend.remoteCall.api.dto.response.RemoteResponse;
import com.agentnexus.backend.remoteCall.api.dto.response.SuperAgentDetail;
import com.agentnexus.backend.remoteCall.api.dto.response.SuperAgentInfo;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface RemoteCallServiceClient {
  @GET
  @Path("/models/{pageSize}/{curPage}")
  RemoteResponse<ListResult<ModelInfo>> listModels(
      @PathParam("pageSize") int pageSize,
      @PathParam("curPage") int curPage,
      @HeaderParam("x-space-id") String spaceId);

  @GET
  @Path("/super-agents/{pageSize}/{curPage}")
  RemoteResponse<ListResult<SuperAgentInfo>> listAgents(
      @PathParam("pageSize") int pageSize,
      @PathParam("curPage") int curPage,
      @HeaderParam("x-space-id") String spaceId);

  @GET
  @Path("/super-agents/{superAgentId}")
  RemoteResponse<SuperAgentDetail> getAgentDetail(
      @PathParam("superAgentId") String superAgentId,
      @HeaderParam("x-space-id") String spaceId);

  @GET
  @Path("/super-agents/{superAgentId}/bundles")
  RemoteResponse<AgentBundleListResult> listAgentBundles(
      @PathParam("superAgentId") String superAgentId,
      @HeaderParam("x-space-id") String spaceId);
}
