import { http, unwrap, type ApiResponse } from './http'
import type {
  AnnotationDetail,
  AppType,
  EvaluatorSource,
  MappingSourceType,
  PageResponse,
  TaskDetail,
  TaskStatus,
  TaskSummary
} from '../types'

export interface AppFieldMappingPayload {
  appInputId: string
  appInputName: string
  appInputType: string
  datasetFieldId: string
}

export interface TaskEvaluatorParamMappingPayload {
  paramId?: string
  paramName: string
  sourceType: MappingSourceType
  datasetFieldId?: string
  appOutputName?: string
}

export interface TaskEvaluatorPayload {
  evaluatorSource: EvaluatorSource
  evaluatorId: string
  evaluatorVersionId?: string
  modelId?: string
  paramMappings: TaskEvaluatorParamMappingPayload[]
}

export interface CreateTaskPayload {
  taskName: string
  description: string
  datasetId: string
  datasetVersionId: string
  appType: AppType
  appId: string
  appVersionId: string
  appAgentAlias: string
  appFieldMappings: AppFieldMappingPayload[]
  evaluators: TaskEvaluatorPayload[]
  tagIds: string[]
}

export interface SaveAnnotationPayload {
  tags: Array<{
    taskTagId: string
    valueText?: string
    valueNumber?: number
    tagOptionId?: string
  }>
}

export const taskApi = {
  listTasks(params: {
    page: number
    size: number
    status?: TaskStatus | ''
    keyword?: string
    sortBy?: 'createdDate' | 'lastUpdatedDate'
    sortOrder?: 'asc' | 'desc'
  }) {
    return unwrap(http.get<ApiResponse<PageResponse<TaskSummary>>>('/tasks', { params }))
  },
  createTask(data: CreateTaskPayload) {
    return unwrap(http.post<ApiResponse<TaskDetail>>('/tasks', data))
  },
  getTask(taskId: string, params: { page: number; size: number }) {
    return unwrap(http.get<ApiResponse<TaskDetail>>(`/tasks/${taskId}`, { params }))
  },
  startTask(taskId: string) {
    return unwrap(http.post<ApiResponse<TaskDetail>>(`/tasks/${taskId}/start`))
  },
  deleteTask(taskId: string) {
    return unwrap(http.delete<ApiResponse<void>>(`/tasks/${taskId}`))
  },
  getAnnotation(taskId: string, taskItemId: string) {
    return unwrap(http.get<ApiResponse<AnnotationDetail>>(`/tasks/${taskId}/items/${taskItemId}/annotation`))
  },
  saveAnnotation(taskId: string, taskItemId: string, data: SaveAnnotationPayload) {
    return unwrap(http.put<ApiResponse<AnnotationDetail>>(`/tasks/${taskId}/items/${taskItemId}/annotation`, data))
  }
}
