import axios from 'axios'
import type { AxiosResponse } from 'axios'
import type {
  EvaluatorConfig,
  EvaluatorParam,
  EvaluatorSummary,
  EvaluatorType,
  EvaluatorVersion,
  PageResponse,
  PresetCategory,
  PresetEvaluatorDetail,
  PresetEvaluatorSummary
} from '../types'

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
  return request.then((res) => {
    if (res.data.code !== 0) {
      throw new Error(res.data.msg)
    }
    return res.data.data
  })
}

export interface SaveEvaluatorPayload {
  evaluatorName: string
  evaluatorType: EvaluatorType
  description: string
  modelId: string
  prompt: string
  executeCode: string
  scoreMin: number
  scoreMax: number
  passThreshold: number
  params: EvaluatorParam[]
}

export const evaluatorApi = {
  listEvaluators(params: { page: number; size: number; evaluatorType?: EvaluatorType | ''; keyword?: string }) {
    return unwrap(http.get<ApiResponse<PageResponse<EvaluatorSummary>>>('/evaluators', { params }))
  },
  listPresetCategories() {
    return unwrap(http.get<ApiResponse<PresetCategory[]>>('/evaluators/presets/categories'))
  },
  listPresetEvaluators(params: { page: number; size: number; categoryId?: string; keyword?: string }) {
    return unwrap(http.get<ApiResponse<PageResponse<PresetEvaluatorSummary>>>('/evaluators/presets', { params }))
  },
  getPresetEvaluator(presetId: string) {
    return unwrap(http.get<ApiResponse<PresetEvaluatorDetail>>(`/evaluators/presets/${presetId}`))
  },
  createEvaluator(data: SaveEvaluatorPayload) {
    return unwrap(http.post<ApiResponse<EvaluatorConfig>>('/evaluators', data))
  },
  copyEvaluator(evaluatorId: string) {
    return unwrap(http.post<ApiResponse<EvaluatorConfig>>(`/evaluators/${evaluatorId}/copy`))
  },
  deleteEvaluator(evaluatorId: string) {
    return unwrap(http.delete<ApiResponse<void>>(`/evaluators/${evaluatorId}`))
  },
  listVersions(evaluatorId: string) {
    return unwrap(http.get<ApiResponse<EvaluatorVersion[]>>(`/evaluators/${evaluatorId}/versions`))
  },
  publish(evaluatorId: string) {
    return unwrap(http.post<ApiResponse<EvaluatorConfig>>(`/evaluators/${evaluatorId}/publish`))
  },
  getVersion(versionId: string) {
    return unwrap(http.get<ApiResponse<EvaluatorConfig>>(`/evaluators/versions/${versionId}`))
  },
  updateDraft(versionId: string, data: SaveEvaluatorPayload) {
    return unwrap(http.put<ApiResponse<EvaluatorConfig>>(`/evaluators/versions/${versionId}`, data))
  }
}
