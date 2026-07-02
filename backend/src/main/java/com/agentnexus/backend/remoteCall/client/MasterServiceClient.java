package com.agentnexus.backend.remoteCall.client;

import com.agentnexus.backend.integration.api.dto.response.PlatformAgentBundleListResponse;
import com.agentnexus.backend.integration.api.dto.response.PlatformAgentDetailResponse;
import com.agentnexus.backend.integration.api.dto.response.PlatformAgentListResponse;
import com.agentnexus.backend.integration.api.dto.response.PlatformModelListResponse;
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
  PlatformModelListResponse listModels(
      @PathParam("pageSize") int pageSize,
      @PathParam("curPage") int curPage,
      @HeaderParam("x-space-id") String spaceId);

  @GET
  @Path("/super-agents/{pageSize}/{curPage}")
  PlatformAgentListResponse listAgents(
      @PathParam("pageSize") int pageSize,
      @PathParam("curPage") int curPage,
      @HeaderParam("x-space-id") String spaceId);

  @GET
  @Path("/super-agents/{superAgentId}")
  PlatformAgentDetailResponse getAgentDetail(
      @PathParam("superAgentId") String superAgentId,
      @HeaderParam("x-space-id") String spaceId);

  @GET
  @Path("/super-agents/{superAgentId}/{bundles}")
  PlatformAgentBundleListResponse listAgentBundles(
      @PathParam("superAgentId") String superAgentId,
      @PathParam("bundles") String bundles,
      @HeaderParam("x-space-id") String spaceId);
}
