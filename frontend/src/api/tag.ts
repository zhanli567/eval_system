import axios from 'axios'
import type { AxiosResponse } from 'axios'
import type { PageResponse, TagDetail, TagOption, TagSummary, TagType } from '../types'

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
