package com.agentnexus.backend.remoteCall.client;

import com.agentnexus.backend.remoteCall.api.dto.response.AgentBundleItem;
import com.agentnexus.backend.remoteCall.api.dto.response.AgentBundleListResult;
import com.agentnexus.backend.remoteCall.api.dto.response.ListResult;
import com.agentnexus.backend.remoteCall.api.dto.response.LoadedAgent;
import com.agentnexus.backend.remoteCall.api.dto.response.ModelInfo;
import com.agentnexus.backend.remoteCall.api.dto.response.RemoteResponse;
import com.agentnexus.backend.remoteCall.api.dto.response.SpaceInfo;
import com.agentnexus.backend.remoteCall.api.dto.response.SuperAgentDetail;
import com.agentnexus.backend.remoteCall.api.dto.response.SuperAgentInfo;
import java.util.List;
import java.util.Map;

public class LocalMockRemoteCallServiceClient implements RemoteCallServiceClient {
  private static final String STATUS_OK = "200";
  private static final String DEFAULT_SPACE_ID = "local-space";
  private static final String MOCK_TIME = "2026-01-01T00:00:00Z";
  private static final String AGENT_ID = "local-mock-agent";
  private static final String BUNDLE_ID = "local-mock-bundle-v1";
  private static final List<ModelInfo> MODELS = List.of(
      new ModelInfo(
          "local-mock-model",
          "Local Mock Model",
          "Local",
          "local-mock-model",
          "mock://model",
          "local",
          List.of("chat", "evaluation"),
          "IAM",
          "ACTIVE",
          MOCK_TIME,
          MOCK_TIME),
      new ModelInfo(
          "local-mock-fast-model",
          "Local Mock Fast Model",
          "Local",
          "local-mock-fast-model",
          "mock://model-fast",
          "local",
          List.of("chat", "evaluation"),
          "IAM",
          "ACTIVE",
          MOCK_TIME,
          MOCK_TIME));

  @Override
  public RemoteResponse<ListResult<ModelInfo>> listModels(int pageSize, int curPage, String spaceId) {
    return ok("mock://models", MODELS);
  }

  @Override
  public RemoteResponse<ListResult<SuperAgentInfo>> listAgents(int pageSize, int curPage, String spaceId) {
    String resolvedSpaceId = firstNonBlank(spaceId, DEFAULT_SPACE_ID);
    return ok("mock://super-agents", List.of(new SuperAgentInfo(
        AGENT_ID,
        "local-mock-agent",
        "Local Mock Agent",
        "PUBLISHED",
        1,
        1,
        "v1.0.0",
        "local-mock-digest",
        "SYNCED",
        Map.of("mock", true),
        1,
        resolvedSpaceId,
        BUNDLE_ID,
        "Local development agent for running evaluation tasks without remote services.",
        "")));
  }

  @Override
  public RemoteResponse<SuperAgentDetail> getAgentDetail(String superAgentId, String spaceId) {
    String resolvedAgentId = firstNonBlank(superAgentId, AGENT_ID);
    String resolvedSpaceId = firstNonBlank(spaceId, DEFAULT_SPACE_ID);
    return new RemoteResponse<>(
        STATUS_OK,
        "mock://super-agents/" + resolvedAgentId,
        new SuperAgentDetail(
            resolvedAgentId,
            "local-mock-agent",
            "Local Mock Agent",
            "Local development agent for running evaluation tasks without remote services.",
            "local-app",
            "local-sub-app",
            "mock://agent",
            "PUBLISHED",
            "",
            BUNDLE_ID,
            1,
            1,
            "v1.0.0",
            "local-mock-digest",
            "SYNCED",
            List.of(),
            Map.of("mock", true),
            List.of(new LoadedAgent("v1.0.0", "Local QA Agent", "qa-agent", 1, "*")),
            resolvedSpaceId,
            true,
            "local-mock-model",
            1),
        true);
  }

  @Override
  public RemoteResponse<AgentBundleListResult> listAgentBundles(String superAgentId, String spaceId) {
    String resolvedAgentId = firstNonBlank(superAgentId, AGENT_ID);
    return new RemoteResponse<>(
        STATUS_OK,
        "mock://super-agents/" + resolvedAgentId + "/bundles",
        new AgentBundleListResult(
            resolvedAgentId,
            BUNDLE_ID,
            List.of(new AgentBundleItem(
                BUNDLE_ID,
                "PUBLISHED",
                "v1.0.0",
                "local-mock-digest",
                MOCK_TIME))),
        true);
  }

  @Override
  public RemoteResponse<ListResult<SpaceInfo>> listSpaces(int pageSize, int curPage, String cookie) {
    return ok("mock://spaces", List.of(new SpaceInfo(
        DEFAULT_SPACE_ID,
        "Local Mock Space",
        "Local development workspace.",
        "local-user",
        "ACTIVE",
        "1",
        MOCK_TIME,
        MOCK_TIME,
        "local-app")));
  }

  private static <T> RemoteResponse<ListResult<T>> ok(String url, List<T> items) {
    return new RemoteResponse<>(STATUS_OK, url, new ListResult<>(null, items), true);
  }

  private static String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value.trim();
      }
    }
    return "";
  }
}
