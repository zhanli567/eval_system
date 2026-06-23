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
  createdDate: string
  lastUpdatedDate: string
}

export interface DatasetVersion {
  id: string
  datasetId: string
  versionNo: number
  versionName: string
  itemCount: number
  draft: boolean
  createdDate: string
  lastUpdatedDate: string
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
  createdDate: string
  lastUpdatedDate: string
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
  createdDate: string
  lastUpdatedDate: string
}

export interface TagOption {
  id?: string
  tagId?: string
  optionName: string
  optionGroup: TagOptionGroup
  displayOrder?: number
  createdDate?: string
  lastUpdatedDate?: string
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
  required: boolean
  description: string
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
  createdDate: string
  lastUpdatedDate: string
}

export interface EvaluatorVersion {
  id: string
  evaluatorId: string
  versionNo: number
  versionName: string
  draft: boolean
  createdDate: string
  lastUpdatedDate: string
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
  createdDate: string
  lastUpdatedDate: string
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
  createdDate: string
  lastUpdatedDate: string
  params: EvaluatorParam[]
}

export type TaskStatus = 'pending' | 'running' | 'completed' | 'failed'

export type TaskItemStatus = 'pending' | 'running' | 'annotation_pending' | 'completed' | 'failed'

export type AppType = 'none' | 'agent'

export type EvaluatorSource = 'preset' | 'custom'

export type MappingSourceType = 'dataset_field' | 'app_output'

export interface TaskBase {
  id: string
  taskName: string
  status: TaskStatus
  description: string
  datasetId: string
  datasetName: string
  datasetVersionId: string
  datasetVersionNo: number
  datasetVersionName: string
  itemCount: number
  appType: AppType
  appId: string
  appVersionId: string
  appAgentAlias: string
  startedAt: string
  finishedAt: string
  createdDate: string
  lastUpdatedDate: string
}

export interface TaskEvaluatorDimension {
  taskEvaluatorId: string
  evaluatorSource: EvaluatorSource
  evaluatorId: string
  evaluatorVersionId: string
  evaluatorName: string
  evaluatorType: EvaluatorType
  versionName: string
  status: string
  passCount: number
  completedCount: number
  totalCount: number
  passRate?: number
  displayOrder: number
}

export interface TaskTagDimension {
  taskTagId: string
  tagId: string
  tagName: string
  tagType: TagType
  status: string
  passCount: number
  completedCount: number
  totalCount: number
  passRate?: number
  displayOrder: number
}

export interface TaskSummary {
  base: TaskBase
  evaluators: TaskEvaluatorDimension[]
  tags: TaskTagDimension[]
}

export interface TaskEvaluatorResult {
  id: string
  taskItemId: string
  taskEvaluatorId: string
  evaluatorName: string
  evaluatorType: EvaluatorType
  versionName: string
  status: string
  score?: number
  passResult: 'pass' | 'fail' | ''
  resultValue: string
  errorMessage: string
  startedAt: string
  finishedAt: string
}

export interface TaskTagResult {
  id: string
  taskItemId: string
  taskTagId: string
  tagId: string
  tagName: string
  tagType: TagType
  status: string
  valueText: string
  valueNumber?: number
  tagOptionId: string
  optionName: string
  passResult: 'pass' | 'fail' | ''
  annotatedAt: string
}

export interface TaskItemDetail {
  id: string
  datasetItemId: string
  rowNo: number
  status: TaskItemStatus
  values: Record<string, string>
  appOutput: string
  appOutputStatus: string
  appErrorMessage: string
  evaluatorResults: TaskEvaluatorResult[]
  tagResults: TaskTagResult[]
  createdDate: string
  lastUpdatedDate: string
}

export interface TaskDetail {
  base: TaskBase
  fields: DatasetField[]
  evaluators: TaskEvaluatorDimension[]
  tags: TaskTagDimension[]
  items: PageResponse<TaskItemDetail>
}

export interface TaskTagAnnotation {
  taskTagId: string
  tagId: string
  tagName: string
  tagType: TagType
  description: string
  minValue?: number
  maxValue?: number
  passThreshold?: number
  options: TagOption[]
  result?: TaskTagResult
}

export interface AnnotationDetail {
  task: TaskBase
  item: TaskItemDetail
  fields: DatasetField[]
  tags: TaskTagAnnotation[]
  evaluators: TaskEvaluatorResult[]
  previousItemId?: string
  nextItemId?: string
}
