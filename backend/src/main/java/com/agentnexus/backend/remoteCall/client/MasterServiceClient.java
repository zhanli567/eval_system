package com.agentnexus.backend.remoteCall.client;

import com.agentnexus.backend.remoteCall.api.dto.response.PlatformAgentBundleListResult;
import com.agentnexus.backend.remoteCall.api.dto.response.PlatformListResult;
import com.agentnexus.backend.remoteCall.api.dto.response.PlatformModelInfo;
import com.agentnexus.backend.remoteCall.api.dto.response.PlatformRemoteResponse;
import com.agentnexus.backend.remoteCall.api.dto.response.PlatformSuperAgentDetail;
import com.agentnexus.backend.remoteCall.api.dto.response.PlatformSuperAgentInfo;
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
public interface MasterServiceClient {
  @GET
  @Path("/models/{pageSize}/{curPage}")
  PlatformRemoteResponse<PlatformListResult<PlatformModelInfo>> listModels(
      @PathParam("pageSize") int pageSize,
      @PathParam("curPage") int curPage,
      @HeaderParam("x-space-id") String spaceId);

  @GET
  @Path("/super-agents/{pageSize}/{curPage}")
  PlatformRemoteResponse<PlatformListResult<PlatformSuperAgentInfo>> listAgents(
      @PathParam("pageSize") int pageSize,
      @PathParam("curPage") int curPage,
      @HeaderParam("x-space-id") String spaceId);

  @GET
  @Path("/super-agents/{superAgentId}")
  PlatformRemoteResponse<PlatformSuperAgentDetail> getAgentDetail(
      @PathParam("superAgentId") String superAgentId,
      @HeaderParam("x-space-id") String spaceId);

  @GET
  @Path("/super-agents/{superAgentId}/bundles")
  PlatformRemoteResponse<PlatformAgentBundleListResult> listAgentBundles(
      @PathParam("superAgentId") String superAgentId,
      @HeaderParam("x-space-id") String spaceId);
}
