import { http, unwrap, type ApiResponse } from './http'

export interface PlatformModelInfo {
  modelId: string
  name: string
  provider: string
  modelName: string
  baseUrlRef: string
  timeoutPolicy: string
  capabilities: string[]
  authType: string
  status: string
  createAt: string
  updateAt: string
}

export interface PlatformAgentField {
  id: string
  fieldName: string
  fieldType: string
  description: string
  displayOrder: number
}

export interface PlatformAgentVersion {
  id: string
  versionName: string
}

export interface PlatformAgentChild {
  agentAlias: string
  agentName: string
  version: string
  routePattern: string
}

export interface PlatformAgentDefinition {
  id: string
  agentName: string
  description: string
  iconUrl: string
  versions: PlatformAgentVersion[]
  childAgents: PlatformAgentChild[]
  inputs: PlatformAgentField[]
  outputs: PlatformAgentField[]
}

export interface PlatformModelChatResult {
  modelId: string
  outputText: string
  checkedAt: string
}

export const integrationApi = {
  listModels() {
    return unwrap(http.get<ApiResponse<PlatformModelInfo[]>>('/integration/models'))
  },
  listAgents() {
    return unwrap(http.get<ApiResponse<PlatformAgentDefinition[]>>('/integration/agents'))
  },
  getAgentDetail(agentId: string) {
    return unwrap(http.get<ApiResponse<PlatformAgentDefinition>>(`/integration/agents/${encodeURIComponent(agentId)}`))
  },
  listAgentBundles(agentId: string) {
    return unwrap(http.get<ApiResponse<PlatformAgentVersion[]>>(`/integration/agents/${encodeURIComponent(agentId)}/bundles`))
  },
  chatModel(modelId: string, message: string) {
    return unwrap(http.post<ApiResponse<PlatformModelChatResult>>(`/integration/models/${encodeURIComponent(modelId)}/chat`, { message }))
  }
}
