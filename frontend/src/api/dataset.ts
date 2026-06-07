import axios from 'axios'
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

export const datasetApi = {
  listDatasets(params: { page: number; size: number; keyword?: string }) {
    return http.get<PageResponse<DatasetSummary>>('/datasets', { params }).then((res) => res.data)
  },
  createDataset(data: { name: string; description: string; fields: DatasetField[] }) {
    return http.post<DatasetSummary>('/datasets', data).then((res) => res.data)
  },
  deleteDataset(datasetId: string) {
    return http.delete(`/datasets/${datasetId}`)
  },
  listVersions(datasetId: string) {
    return http.get<DatasetVersion[]>(`/datasets/${datasetId}/versions`).then((res) => res.data)
  },
  getVersionDetail(versionId: string, params: { page: number; size: number; fieldId?: string; keyword?: string }) {
    return http.get<VersionDetail>(`/datasets/versions/${versionId}`, { params }).then((res) => res.data)
  },
  replaceFields(versionId: string, fields: DatasetField[]) {
    return http.put<DatasetField[]>(`/datasets/versions/${versionId}/fields`, fields).then((res) => res.data)
  },
  addRow(versionId: string, values: Record<string, string>) {
    return http.post<DatasetRow>(`/datasets/versions/${versionId}/items`, { values }).then((res) => res.data)
  },
  addRows(versionId: string, rows: Record<string, string>[]) {
    return http.post<DatasetRow[]>(`/datasets/versions/${versionId}/items/batch`, { rows }).then((res) => res.data)
  },
  updateRow(versionId: string, itemId: string, values: Record<string, string>) {
    return http.put<DatasetRow>(`/datasets/versions/${versionId}/items/${itemId}`, { values }).then((res) => res.data)
  },
  deleteRow(versionId: string, itemId: string) {
    return http.delete(`/datasets/versions/${versionId}/items/${itemId}`)
  },
  publish(datasetId: string) {
    return http.post<DatasetVersion>(`/datasets/${datasetId}/publish`).then((res) => res.data)
  },
  deleteVersion(versionId: string) {
    return http.delete(`/datasets/versions/${versionId}`)
  },
  coverDraft(datasetId: string, versionId: string) {
    return http.post<DatasetVersion>(`/datasets/${datasetId}/versions/${versionId}/cover-draft`).then((res) => res.data)
  }
}
