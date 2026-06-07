import axios from 'axios'
import type { AxiosResponse } from 'axios'
import type {
  DatasetField,
  DatasetRow,
  DatasetSummary,
  DatasetVersion,
  PageResponse,
  VersionDetail
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

export const datasetApi = {
  listDatasets(params: { page: number; size: number; keyword?: string }) {
    return unwrap(http.get<ApiResponse<PageResponse<DatasetSummary>>>('/datasets', { params }))
  },
  createDataset(data: { name: string; description: string; fields: DatasetField[] }) {
    return unwrap(http.post<ApiResponse<DatasetSummary>>('/datasets', data))
  },
  deleteDataset(datasetId: string) {
    return unwrap(http.delete<ApiResponse<void>>(`/datasets/${datasetId}`))
  },
  listVersions(datasetId: string) {
    return unwrap(http.get<ApiResponse<DatasetVersion[]>>(`/datasets/${datasetId}/versions`))
  },
  getVersionDetail(versionId: string, params: { page: number; size: number; fieldId?: string; keyword?: string }) {
    return unwrap(http.get<ApiResponse<VersionDetail>>(`/datasets/versions/${versionId}`, { params }))
  },
  replaceFields(versionId: string, fields: DatasetField[]) {
    return unwrap(http.put<ApiResponse<DatasetField[]>>(`/datasets/versions/${versionId}/fields`, fields))
  },
  addRow(versionId: string, values: Record<string, string>) {
    return unwrap(http.post<ApiResponse<DatasetRow>>(`/datasets/versions/${versionId}/items`, { values }))
  },
  addRows(versionId: string, rows: Record<string, string>[]) {
    return unwrap(http.post<ApiResponse<DatasetRow[]>>(`/datasets/versions/${versionId}/items/batch`, { rows }))
  },
  importRows(versionId: string, file: File) {
    const formData = new FormData()
    formData.append('file', file)
    return unwrap(http.post<ApiResponse<{ importedCount: number }>>(`/datasets/versions/${versionId}/items/import`, formData))
  },
  coverRowsByExcel(versionId: string, file: File) {
    const formData = new FormData()
    formData.append('file', file)
    return unwrap(http.post<ApiResponse<{ importedCount: number }>>(`/datasets/versions/${versionId}/items/import-cover`, formData))
  },
  updateRow(versionId: string, itemId: string, values: Record<string, string>) {
    return unwrap(http.put<ApiResponse<DatasetRow>>(`/datasets/versions/${versionId}/items/${itemId}`, { values }))
  },
  deleteRow(versionId: string, itemId: string) {
    return unwrap(http.delete<ApiResponse<void>>(`/datasets/versions/${versionId}/items/${itemId}`))
  },
  publish(datasetId: string) {
    return unwrap(http.post<ApiResponse<DatasetVersion>>(`/datasets/${datasetId}/publish`))
  },
  deleteVersion(versionId: string) {
    return unwrap(http.delete<ApiResponse<void>>(`/datasets/versions/${versionId}`))
  },
  coverDraft(datasetId: string, versionId: string) {
    return unwrap(http.post<ApiResponse<DatasetVersion>>(`/datasets/${datasetId}/versions/${versionId}/cover-draft`))
  }
}
