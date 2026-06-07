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
  fieldType: 'string' | 'number' | 'boolean' | 'json'
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
