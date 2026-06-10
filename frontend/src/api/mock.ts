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

export interface MockAgentField {
  id: string
  fieldName: string
  fieldType: string
  description: string
  displayOrder: number
}

export interface MockAgentVersion {
  id: string
  versionName: string
}

export interface MockAgentDefinition {
  id: string
  agentName: string
  description: string
  versions: MockAgentVersion[]
  inputs: MockAgentField[]
  outputs: MockAgentField[]
}

export const mockApi = {
  listAgents() {
    return unwrap(http.get<ApiResponse<MockAgentDefinition[]>>('/mock/agents'))
  }
}
