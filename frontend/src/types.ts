export interface PageResponse<T> {
  records: T[]
  total: number
  page: number
  size: number
}

export interface DatasetSummary {
  id: string
  name: string
  description: string
  publishedVersionCount: number
  latestPublishedVersionId?: string
  latestItemCount: number
  createdAt: string
  updatedAt: string
}

export interface DatasetVersion {
  id: string
  datasetId: string
  versionNo: number
  versionName: string
  itemCount: number
  draft: boolean
  createdAt: string
  updatedAt: string
}

export interface DatasetField {
  id?: string
  versionId?: string
  fieldName: string
  fieldType: 'string' | 'number' | 'boolean'
  required: boolean
  description: string
  displayOrder?: number
}

export interface DatasetRow {
  id: string
  rowNo: number
  values: Record<string, string>
  createdAt: string
  updatedAt: string
}

export interface VersionDetail {
  version: DatasetVersion
  fields: DatasetField[]
  rows: PageResponse<DatasetRow>
}

export type TagType = 'category' | 'boolean' | 'number' | 'text'

export type TagOptionGroup = 'pass' | 'fail'

export interface TagSummary {
  id: string
  tagName: string
  tagType: TagType
  description: string
  createdAt: string
  updatedAt: string
}

export interface TagOption {
  id?: string
  tagId?: string
  optionName: string
  optionGroup: TagOptionGroup
  displayOrder?: number
  createdAt?: string
  updatedAt?: string
}

export interface TagDetail extends TagSummary {
  minValue?: number
  maxValue?: number
  passThreshold?: number
  options: TagOption[]
}
