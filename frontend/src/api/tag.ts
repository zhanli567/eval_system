import { http, unwrap, type ApiResponse } from './http'
import type { PageResponse, TagDetail, TagOption, TagSummary, TagType } from '../types'

export interface SaveTagPayload {
  tagName: string
  tagType: TagType
  description: string
  minValue?: number
  maxValue?: number
  passThreshold?: number
  options?: TagOption[]
}

export const tagApi = {
  listTags(params: { page: number; size: number; tagType?: TagType | ''; keyword?: string }) {
    return unwrap(http.get<ApiResponse<PageResponse<TagSummary>>>('/tags', { params }))
  },
  getTag(tagId: string) {
    return unwrap(http.get<ApiResponse<TagDetail>>(`/tags/${tagId}`))
  },
  createTag(data: SaveTagPayload) {
    return unwrap(http.post<ApiResponse<TagDetail>>('/tags', data))
  },
  updateTag(tagId: string, data: SaveTagPayload) {
    return unwrap(http.put<ApiResponse<TagDetail>>(`/tags/${tagId}`, data))
  }
}
