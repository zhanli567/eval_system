package com.evalsystem.integration.service;

import com.evalsystem.integration.dto.PlatformAgentChatRequest;
import com.evalsystem.integration.dto.PlatformAgentChatResponse;
import com.evalsystem.integration.dto.PlatformAgentDefinition;
import com.evalsystem.integration.dto.PlatformModelChatResult;
import com.evalsystem.integration.dto.PlatformModelInfo;
import java.util.List;

public interface PlatformIntegrationService {
  List<PlatformModelInfo> listModels();

  List<PlatformAgentDefinition> listAgents();

  PlatformModelChatResult chatModel(String modelId, String message);

  PlatformAgentChatResponse invokeAgent(String agentAlias, PlatformAgentChatRequest request);
}
