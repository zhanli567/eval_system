<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Plus, Refresh } from '@element-plus/icons-vue'
import { datasetApi } from './api/dataset'
import type { DatasetField, DatasetRow, DatasetSummary, DatasetVersion, VersionDetail } from './types'

const datasetLoading = ref(false)
const detailLoading = ref(false)
const datasets = ref<DatasetSummary[]>([])
const datasetTotal = ref(0)
const datasetPage = ref(1)
const datasetSize = ref(8)
const datasetKeyword = ref('')
const selectedDataset = ref<DatasetSummary>()

const versions = ref<DatasetVersion[]>([])
const activeVersionId = ref('')
const detail = ref<VersionDetail>()
const tablePage = ref(1)
const tableSize = ref(8)
const searchFieldId = ref('')
const searchKeyword = ref('')

const createVisible = ref(false)
const fieldVisible = ref(false)
const rowVisible = ref(false)
const batchVisible = ref(false)
const rowEditingId = ref('')
const batchText = ref('')

const createForm = reactive({
  name: '',
  description: '',
  fields: [
    { fieldName: 'query', fieldType: 'string', required: true, description: '用户问题' },
    { fieldName: 'reference_response', fieldType: 'string', required: false, description: '参考答案' }
  ] as DatasetField[]
})

const fieldForm = ref<DatasetField[]>([])
const rowForm = reactive<Record<string, string>>({})

const activeVersion = computed(() => detail.value?.version)
const isDraft = computed(() => activeVersion.value?.draft === true)
const tableRows = computed(() => detail.value?.rows.records ?? [])
const tableTotal = computed(() => detail.value?.rows.total ?? 0)
const fields = computed(() => detail.value?.fields ?? [])

onMounted(async () => {
  await loadDatasets()
})

async function loadDatasets() {
  datasetLoading.value = true
  try {
    const page = await datasetApi.listDatasets({
      page: datasetPage.value,
      size: datasetSize.value,
      keyword: datasetKeyword.value
    })
    datasets.value = page.records
    datasetTotal.value = page.total
    if (!selectedDataset.value && datasets.value.length) {
      await selectDataset(datasets.value[0])
    }
  } finally {
    datasetLoading.value = false
  }
}

async function selectDataset(dataset: DatasetSummary) {
  selectedDataset.value = dataset
  activeVersionId.value = ''
  tablePage.value = 1
  searchFieldId.value = ''
  searchKeyword.value = ''
  await loadVersions()
}

async function loadVersions(preferredVersionId?: string) {
  if (!selectedDataset.value) return
  versions.value = await datasetApi.listVersions(selectedDataset.value.id)
  const preferred = versions.value.find((item) => item.id === preferredVersionId)
  const fallback = versions.value.find((item) => item.draft) ?? versions.value[0]
  if (preferred ?? fallback) {
    await selectVersion((preferred ?? fallback).id)
  } else {
    detail.value = undefined
  }
}

async function selectVersion(versionId: string) {
  activeVersionId.value = versionId
  tablePage.value = 1
  await loadDetail()
}

async function loadDetail() {
  if (!activeVersionId.value) return
  detailLoading.value = true
  try {
    detail.value = await datasetApi.getVersionDetail(activeVersionId.value, {
      page: tablePage.value,
      size: tableSize.value,
      fieldId: searchFieldId.value || undefined,
      keyword: searchKeyword.value || undefined
    })
  } finally {
    detailLoading.value = false
  }
}

function openCreateDialog() {
  createForm.name = ''
  createForm.description = ''
  createForm.fields = [
    { fieldName: 'query', fieldType: 'string', required: true, description: '用户问题' },
    { fieldName: 'reference_response', fieldType: 'string', required: false, description: '参考答案' }
  ]
  createVisible.value = true
}

async function submitCreate() {
  if (!createForm.name.trim()) {
    ElMessage.warning('请输入评测集名称')
    return
  }
  if (!createForm.fields.length || createForm.fields.some((field) => !field.fieldName.trim())) {
    ElMessage.warning('请完善表结构')
    return
  }
  const created = await datasetApi.createDataset({
    name: createForm.name,
    description: createForm.description,
    fields: createForm.fields
  })
  createVisible.value = false
  ElMessage.success('评测集已创建')
  await loadDatasets()
  const match = datasets.value.find((item) => item.id === created.id)
  if (match) await selectDataset(match)
}

async function removeDataset(row: DatasetSummary) {
  await ElMessageBox.confirm(`确定删除评测集「${row.name}」吗？`, '删除评测集', { type: 'warning' })
  await datasetApi.deleteDataset(row.id)
  ElMessage.success('已删除')
  if (selectedDataset.value?.id === row.id) {
    selectedDataset.value = undefined
    detail.value = undefined
    versions.value = []
  }
  await loadDatasets()
}

function addField(target: DatasetField[]) {
  target.push({ fieldName: '', fieldType: 'string', required: false, description: '' })
}

function removeField(target: DatasetField[], index: number) {
  target.splice(index, 1)
}

function openFieldDialog() {
  fieldForm.value = fields.value.map((field) => ({ ...field }))
  fieldVisible.value = true
}

async function submitFields() {
  if (!activeVersionId.value) return
  if (!fieldForm.value.length || fieldForm.value.some((field) => !field.fieldName.trim())) {
    ElMessage.warning('请完善列名')
    return
  }
  await datasetApi.replaceFields(activeVersionId.value, fieldForm.value)
  fieldVisible.value = false
  ElMessage.success('表头已保存')
  await loadDetail()
}

function openRowDialog(row?: DatasetRow) {
  rowEditingId.value = row?.id ?? ''
  for (const key of Object.keys(rowForm)) {
    delete rowForm[key]
  }
  for (const field of fields.value) {
    if (field.id) {
      rowForm[field.id] = row?.values[field.id] ?? ''
    }
  }
  rowVisible.value = true
}

async function submitRow() {
  if (!activeVersionId.value) return
  if (rowEditingId.value) {
    await datasetApi.updateRow(activeVersionId.value, rowEditingId.value, { ...rowForm })
    ElMessage.success('数据已更新')
  } else {
    await datasetApi.addRow(activeVersionId.value, { ...rowForm })
    ElMessage.success('数据已新增')
  }
  rowVisible.value = false
  await loadDetail()
  await refreshSelectedDataset()
}

async function removeRow(row: DatasetRow) {
  if (!activeVersionId.value) return
  await ElMessageBox.confirm(`确定删除第 ${row.rowNo} 行吗？`, '删除数据', { type: 'warning' })
  await datasetApi.deleteRow(activeVersionId.value, row.id)
  ElMessage.success('已删除')
  await loadDetail()
  await refreshSelectedDataset()
}

async function submitBatchRows() {
  if (!activeVersionId.value) return
  const rows = parseBatchRows()
  if (!rows.length) {
    ElMessage.warning('请输入批量数据')
    return
  }
  await datasetApi.addRows(activeVersionId.value, rows)
  batchVisible.value = false
  batchText.value = ''
  ElMessage.success(`已新增 ${rows.length} 行`)
  await loadDetail()
  await refreshSelectedDataset()
}

function parseBatchRows() {
  const lines = batchText.value.split('\n').map((line) => line.trim()).filter(Boolean)
  return lines.map((line) => {
    const values = line.split('\t')
    const record: Record<string, string> = {}
    fields.value.forEach((field, index) => {
      if (field.id) record[field.id] = values[index] ?? ''
    })
    return record
  })
}

async function publishDraft() {
  if (!selectedDataset.value) return
  await ElMessageBox.confirm('发布后将生成新的只读版本，确定发布当前草稿吗？', '发布版本', { type: 'success' })
  const version = await datasetApi.publish(selectedDataset.value.id)
  ElMessage.success(`已发布 ${version.versionName}`)
  await loadDatasets()
  await loadVersions(version.id)
}

async function removeVersion(version: DatasetVersion) {
  await ElMessageBox.confirm(`确定删除 ${version.versionName} 吗？`, '删除版本', { type: 'warning' })
  await datasetApi.deleteVersion(version.id)
  ElMessage.success('版本已删除')
  await loadDatasets()
  await loadVersions()
}

async function coverDraft(version: DatasetVersion) {
  if (!selectedDataset.value) return
  await ElMessageBox.confirm(`确定用 ${version.versionName} 全量覆盖草稿吗？`, '覆盖草稿', { type: 'warning' })
  const draft = await datasetApi.coverDraft(selectedDataset.value.id, version.id)
  ElMessage.success('草稿已覆盖')
  await loadVersions(draft.id)
}

async function refreshSelectedDataset() {
  if (!selectedDataset.value) return
  const page = await datasetApi.listDatasets({ page: 1, size: 100 })
  const fresh = page.records.find((item) => item.id === selectedDataset.value?.id)
  if (fresh) selectedDataset.value = fresh
}

function formatTime(value?: string) {
  if (!value) return '-'
  const numberValue = Number(value)
  if (Number.isNaN(numberValue)) return value
  return new Date(numberValue).toLocaleString()
}
</script>

<template>
  <main class="app-shell">
    <header class="topbar">
      <div>
        <p class="eyebrow">应用评测</p>
        <h1>评测集管理</h1>
      </div>
      <div class="top-actions">
        <el-button :icon="Refresh" @click="loadDatasets">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreateDialog">创建评测集</el-button>
      </div>
    </header>

    <section class="dataset-panel">
      <div class="panel-toolbar">
        <el-input
          v-model="datasetKeyword"
          clearable
          placeholder="请输入评测集名称"
          class="search-input"
          @keyup.enter="loadDatasets"
          @clear="loadDatasets"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button @click="loadDatasets">搜索</el-button>
      </div>

      <el-table
        v-loading="datasetLoading"
        :data="datasets"
        row-key="id"
        highlight-current-row
        class="dataset-table"
        @row-click="selectDataset"
      >
        <el-table-column prop="name" label="评测集名称" min-width="220">
          <template #default="{ row }">
            <span class="linkish">{{ row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="publishedVersionCount" label="版本数量" width="110" />
        <el-table-column prop="latestItemCount" label="数据量" width="110" />
        <el-table-column prop="description" label="描述" min-width="260">
          <template #default="{ row }">{{ row.description || '暂无描述' }}</template>
        </el-table-column>
        <el-table-column label="创建时间" width="190">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="更新时间" width="190">
          <template #default="{ row }">{{ formatTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="danger" @click.stop="removeDataset(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager-row">
        <el-pagination
          v-model:current-page="datasetPage"
          v-model:page-size="datasetSize"
          layout="total, prev, pager, next"
          :total="datasetTotal"
          @current-change="loadDatasets"
        />
      </div>
    </section>

    <section v-if="selectedDataset" class="detail-panel">
      <aside class="version-rail">
        <div class="rail-title">
          <span>评测集版本</span>
          <strong>{{ versions.length }}</strong>
        </div>
        <button
          v-for="version in versions"
          :key="version.id"
          class="version-item"
          :class="{ active: activeVersionId === version.id }"
          @click="selectVersion(version.id)"
        >
          <span>{{ version.versionName }}</span>
          <small>{{ version.itemCount }} 条</small>
        </button>
      </aside>

      <div class="version-content" v-loading="detailLoading">
        <div class="version-head">
          <div>
            <p class="eyebrow">{{ selectedDataset.name }}</p>
            <h2>{{ activeVersion?.versionName || '-' }}</h2>
            <span class="meta">数据量 {{ activeVersion?.itemCount ?? 0 }} · {{ isDraft ? '草稿可编辑' : '发布版本只读' }}</span>
          </div>
          <div class="version-actions">
            <template v-if="isDraft">
              <el-button @click="openFieldDialog">编辑表头</el-button>
              <el-button @click="batchVisible = true">批量新增</el-button>
              <el-button type="primary" @click="openRowDialog()">添加数据</el-button>
              <el-button type="success" @click="publishDraft">发布</el-button>
            </template>
            <template v-else-if="activeVersion">
              <el-button @click="coverDraft(activeVersion)">覆盖当前草稿</el-button>
              <el-button type="danger" plain @click="removeVersion(activeVersion)">删除版本</el-button>
            </template>
          </div>
        </div>

        <div class="panel-toolbar">
          <el-select v-model="searchFieldId" clearable placeholder="选择搜索列" class="field-select">
            <el-option v-for="field in fields" :key="field.id" :label="field.fieldName" :value="field.id" />
          </el-select>
          <el-input v-model="searchKeyword" clearable placeholder="请输入关键词" class="search-input" @keyup.enter="loadDetail" />
          <el-button @click="loadDetail">筛选</el-button>
        </div>

        <el-table :data="tableRows" row-key="id" border class="data-table">
          <el-table-column label="序号" width="90">
            <template #default="{ row }"># {{ row.rowNo }}</template>
          </el-table-column>
          <el-table-column
            v-for="field in fields"
            :key="field.id"
            :label="field.fieldName"
            min-width="220"
            show-overflow-tooltip
          >
            <template #header>
              <span>{{ field.fieldName }}</span>
              <el-tag v-if="field.required" size="small" type="danger" effect="plain">必填</el-tag>
            </template>
            <template #default="{ row }">
              {{ row.values[field.id || ''] || '-' }}
            </template>
          </el-table-column>
          <el-table-column v-if="isDraft" label="操作" width="130" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openRowDialog(row)">编辑</el-button>
              <el-button link type="danger" @click="removeRow(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pager-row">
          <el-pagination
            v-model:current-page="tablePage"
            v-model:page-size="tableSize"
            layout="total, prev, pager, next"
            :total="tableTotal"
            @current-change="loadDetail"
          />
        </div>
      </div>
    </section>

    <el-empty v-else description="暂无评测集，创建一个开始演示" />

    <el-dialog v-model="createVisible" title="创建评测集" width="760px">
      <el-form label-position="top">
        <el-form-item label="评测集名称">
          <el-input v-model="createForm.name" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="createForm.description" type="textarea" maxlength="200" show-word-limit />
        </el-form-item>
        <div class="dialog-subtitle">
          <span>表结构</span>
          <el-button link type="primary" :icon="Plus" @click="addField(createForm.fields)">添加列</el-button>
        </div>
        <div v-for="(field, index) in createForm.fields" :key="index" class="field-editor">
          <el-input v-model="field.fieldName" placeholder="列名" />
          <el-select v-model="field.fieldType" placeholder="类型">
            <el-option label="文本" value="string" />
            <el-option label="数字" value="number" />
            <el-option label="布尔" value="boolean" />
            <el-option label="JSON" value="json" />
          </el-select>
          <el-checkbox v-model="field.required">必填</el-checkbox>
          <el-input v-model="field.description" placeholder="描述" />
          <el-button :icon="Delete" circle @click="removeField(createForm.fields, index)" />
        </div>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreate">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="fieldVisible" title="编辑表头" width="780px">
      <div class="dialog-subtitle">
        <span>草稿表结构</span>
        <el-button link type="primary" :icon="Plus" @click="addField(fieldForm)">添加列</el-button>
      </div>
      <div v-for="(field, index) in fieldForm" :key="field.id || index" class="field-editor">
        <el-input v-model="field.fieldName" placeholder="列名" />
        <el-select v-model="field.fieldType" placeholder="类型">
          <el-option label="文本" value="string" />
          <el-option label="数字" value="number" />
          <el-option label="布尔" value="boolean" />
          <el-option label="JSON" value="json" />
        </el-select>
        <el-checkbox v-model="field.required">必填</el-checkbox>
        <el-input v-model="field.description" placeholder="描述" />
        <el-button :icon="Delete" circle @click="removeField(fieldForm, index)" />
      </div>
      <template #footer>
        <el-button @click="fieldVisible = false">取消</el-button>
        <el-button type="primary" @click="submitFields">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rowVisible" :title="rowEditingId ? '编辑数据' : '新增数据'" width="720px">
      <el-form label-position="top">
        <el-form-item v-for="field in fields" :key="field.id" :label="field.fieldName">
          <el-input v-model="rowForm[field.id || '']" type="textarea" :autosize="{ minRows: 2, maxRows: 5 }" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rowVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRow">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="batchVisible" title="批量新增数据" width="720px">
      <p class="hint">每行一条数据，按当前表头顺序用 Tab 分隔。</p>
      <el-input
        v-model="batchText"
        type="textarea"
        :autosize="{ minRows: 8, maxRows: 14 }"
        placeholder="问题1&#9;参考答案1&#10;问题2&#9;参考答案2"
      />
      <template #footer>
        <el-button @click="batchVisible = false">取消</el-button>
        <el-button type="primary" @click="submitBatchRows">保存</el-button>
      </template>
    </el-dialog>
  </main>
</template>
