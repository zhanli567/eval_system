import axios from 'axios'
import type { AxiosResponse } from 'axios'

const http = axios.create({
  baseURL: '/api',
  timeout: 10000
})

interface ApiResponse<T> {
  code: number
  msg: string
  data: T
}

function unwrap<T>(request: Promise<AxiosResponse<ApiResponse<T>>>) {
  return request
    .then((res) => {
      if (res.data.code !== 0) {
        throw new Error(res.data.msg)
      }
      return res.data.data
    })
    .catch((error) => {
      const message = error?.response?.data?.msg || error?.message || '请求失败'
      throw new Error(message)
    })
}

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
  chatModel(modelId: string, message: string) {
    return unwrap(http.post<ApiResponse<PlatformModelChatResult>>(`/integration/models/${encodeURIComponent(modelId)}/chat`, { message }))
  }
}
