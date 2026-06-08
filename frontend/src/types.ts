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

export type EvaluatorType = 'llm' | 'code'

export type EvaluatorParamType = 'string' | 'number' | 'boolean'

export interface EvaluatorParam {
  id?: string
  targetType?: 'preset' | 'version'
  targetId?: string
  paramName: string
  dataType: EvaluatorParamType
  defaultValue: string
  displayOrder?: number
}

export interface EvaluatorSummary {
  id: string
  evaluatorName: string
  evaluatorType: EvaluatorType
  latestVersionId: string
  latestVersionNo: number
  latestVersionName: string
  description: string
  createdAt: string
  updatedAt: string
}

export interface EvaluatorVersion {
  id: string
  evaluatorId: string
  versionNo: number
  versionName: string
  draft: boolean
  createdAt: string
  updatedAt: string
}

export interface EvaluatorConfig {
  evaluatorId: string
  evaluatorName: string
  evaluatorType: EvaluatorType
  description: string
  versionId: string
  versionNo: number
  versionName: string
  draft: boolean
  modelId: string
  prompt: string
  executeCode: string
  scoreMin: number
  scoreMax: number
  passThreshold: number
  createdAt: string
  updatedAt: string
  params: EvaluatorParam[]
}

export interface PresetCategory {
  id: string
  categoryName: string
  displayOrder: number
}

export interface PresetEvaluatorSummary {
  id: string
  categoryId: string
  categoryName: string
  evaluatorName: string
  evaluatorType: EvaluatorType
  description: string
}

export interface PresetEvaluatorDetail extends PresetEvaluatorSummary {
  modelId: string
  prompt: string
  executeCode: string
  scoreMin: number
  scoreMax: number
  passThreshold: number
  createdAt: string
  updatedAt: string
  params: EvaluatorParam[]
}
