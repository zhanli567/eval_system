package com.evalsystem.mock.service;

import com.evalsystem.mock.dto.MockAgentChatRequest;
import com.evalsystem.mock.dto.MockAgentChatResponse;
import com.evalsystem.mock.dto.MockAgentDefinition;
import com.evalsystem.mock.dto.MockEvaluatorRequest;
import com.evalsystem.mock.dto.MockEvaluatorResponse;
import java.util.List;

public interface MockRuntimeService {
  List<MockAgentDefinition> listAgents();

  MockAgentChatResponse invokeAgent(MockAgentChatRequest request);

  MockEvaluatorResponse evaluateEvaluator(MockEvaluatorRequest request);
}
