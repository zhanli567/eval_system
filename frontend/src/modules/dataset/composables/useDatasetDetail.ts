import { computed, reactive, ref, watch, type Ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { AxiosError } from 'axios'
import { datasetApi } from '../../../api/dataset'
import type { DatasetField, DatasetRow, DatasetSummary, DatasetVersion, VersionDetail } from '../../../types'

export function useDatasetDetail(datasetId: Ref<string>) {
  const router = useRouter()
  const detailLoading = ref(false)
  const datasetSummary = ref<DatasetSummary>()
  const versions = ref<DatasetVersion[]>([])
  const activeVersionId = ref('')
  const detail = ref<VersionDetail>()
  const tablePage = ref(1)
  const tableSize = ref(8)
  const searchFieldId = ref('')
  const searchKeyword = ref('')

  const fieldVisible = ref(false)
  const rowVisible = ref(false)
  const rowEditingId = ref('')
  const excelInput = ref<HTMLInputElement>()
  const coverExcelInput = ref<HTMLInputElement>()
  const draggedFieldIndex = ref<number | null>(null)

  const fieldForm = ref<DatasetField[]>([])
  const rowForm = reactive<Record<string, string>>({})

  const datasetTitle = computed(() => datasetSummary.value?.name || '评测集详情')
  const activeVersion = computed(() => detail.value?.version)
  const isDraft = computed(() => activeVersion.value?.draft === true)
  const tableRows = computed(() => detail.value?.rows.records ?? [])
  const tableTotal = computed(() => detail.value?.rows.total ?? 0)
  const fields = computed(() => detail.value?.fields ?? [])
  const dataTableKey = computed(() =>
    fields.value.map((field) => `${field.id}:${field.fieldName}:${field.required}:${field.displayOrder}`).join('|')
  )

  watch(
    datasetId,
    async () => {
      await loadDataset()
    },
    { immediate: true }
  )

  async function loadDataset() {
    detailLoading.value = true
    try {
      await loadDatasetSummary()
      await loadVersions()
    } finally {
      detailLoading.value = false
    }
  }

  async function loadDatasetSummary() {
    const page = await datasetApi.listDatasets({ page: 1, size: 100 })
    datasetSummary.value = page.records.find((item) => item.id === datasetId.value)
  }

  async function loadVersions(preferredVersionId?: string) {
    versions.value = await datasetApi.listVersions(datasetId.value)
    const preferred = versions.value.find((item) => item.id === preferredVersionId)
    const fallback = versions.value.find((item) => item.draft) ?? versions.value[0]
    if (preferred ?? fallback) {
      await selectVersion((preferred ?? fallback).id)
    } else {
      activeVersionId.value = ''
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

  function backToList() {
    router.push({ name: 'datasets' })
  }

  function addField(target: DatasetField[]) {
    target.push({ fieldName: '', fieldType: 'string', required: false, description: '' })
  }

  function removeField(target: DatasetField[], index: number) {
    target.splice(index, 1)
  }

  function startFieldDrag(index: number) {
    draggedFieldIndex.value = index
  }

  function dropField(target: DatasetField[], targetIndex: number) {
    const sourceIndex = draggedFieldIndex.value
    if (sourceIndex === null || sourceIndex === targetIndex) {
      draggedFieldIndex.value = null
      return
    }
    const [moved] = target.splice(sourceIndex, 1)
    target.splice(targetIndex, 0, moved)
    draggedFieldIndex.value = null
  }

  function endFieldDrag() {
    draggedFieldIndex.value = null
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
    const savedFields = await datasetApi.replaceFields(activeVersionId.value, fieldForm.value)
    if (detail.value) {
      detail.value = { ...detail.value, fields: savedFields }
    }
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
    await loadDatasetSummary()
  }

  async function removeRow(row: DatasetRow) {
    if (!activeVersionId.value) return
    await ElMessageBox.confirm(`确定删除第 ${row.rowNo} 行吗？`, '删除数据', { type: 'warning' })
    await datasetApi.deleteRow(activeVersionId.value, row.id)
    ElMessage.success('已删除')
    await loadDetail()
    await loadDatasetSummary()
  }

  async function handleAddDataCommand(command: string | number | object) {
    if (command === 'single') {
      openRowDialog()
      return
    }
    if (command === 'import') {
      openExcelImport()
      return
    }
    if (command === 'cover') {
      await openExcelCover()
    }
  }

  function openExcelImport() {
    if (!activeVersionId.value) return
    if (!fields.value.length) {
      ElMessage.warning('请先维护表头')
      return
    }
    excelInput.value?.click()
  }

  async function openExcelCover() {
    if (!activeVersionId.value) return
    if (!fields.value.length) {
      ElMessage.warning('请先维护表头')
      return
    }
    try {
      await ElMessageBox.confirm('全量覆盖会清空草稿现有数据，并以Excel数据为准，确定继续吗？', '全量覆盖', { type: 'warning' })
      coverExcelInput.value?.click()
    } catch {
      return
    }
  }

  async function importExcel(event: Event) {
    if (!activeVersionId.value) return
    const input = event.target as HTMLInputElement
    const file = input.files?.[0]
    if (!file) return

    try {
      const result = await datasetApi.importRows(activeVersionId.value, file)
      ElMessage.success(`已导入 ${result.importedCount} 行`)
      await loadDetail()
      await loadDatasetSummary()
    } catch (error) {
      ElMessage.error(getErrorMessage(error, '导入失败，请确认Excel表头和当前表头完全一致'))
    } finally {
      input.value = ''
    }
  }

  async function coverExcel(event: Event) {
    if (!activeVersionId.value) return
    const input = event.target as HTMLInputElement
    const file = input.files?.[0]
    if (!file) return

    try {
      const result = await datasetApi.coverRowsByExcel(activeVersionId.value, file)
      ElMessage.success(`已覆盖导入 ${result.importedCount} 行`)
      await loadDetail()
      await loadDatasetSummary()
    } catch (error) {
      ElMessage.error(getErrorMessage(error, '覆盖失败，请确认Excel表头和当前表头完全一致'))
    } finally {
      input.value = ''
    }
  }

  async function publishDraft() {
    await ElMessageBox.confirm('发布后将生成新的只读版本，确定发布当前草稿吗？', '发布版本', { type: 'success' })
    const version = await datasetApi.publish(datasetId.value)
    ElMessage.success(`已发布 ${version.versionName}`)
    await loadDatasetSummary()
    await loadVersions(version.id)
  }

  async function removeVersion(version: DatasetVersion) {
    await ElMessageBox.confirm(`确定删除 ${version.versionName} 吗？`, '删除版本', { type: 'warning' })
    await datasetApi.deleteVersion(version.id)
    ElMessage.success('版本已删除')
    await loadDatasetSummary()
    await loadVersions()
  }

  async function coverDraft(version: DatasetVersion) {
    await ElMessageBox.confirm(`确定用 ${version.versionName} 全量覆盖草稿吗？`, '覆盖草稿', { type: 'warning' })
    const draft = await datasetApi.coverDraft(datasetId.value, version.id)
    ElMessage.success('草稿已覆盖')
    await loadVersions(draft.id)
  }

  function formatTime(value?: string) {
    if (!value) return '-'
    const numberValue = Number(value)
    if (Number.isNaN(numberValue)) return value
    return new Date(numberValue).toLocaleString()
  }

  function getErrorMessage(error: unknown, fallback: string) {
    const axiosError = error as AxiosError<{ msg?: string; message?: string }>
    if (axiosError.response?.data?.msg || axiosError.response?.data?.message) {
      return axiosError.response.data.msg || axiosError.response.data.message || fallback
    }
    return error instanceof Error ? error.message : fallback
  }

  return {
    detailLoading,
    datasetSummary,
    datasetTitle,
    versions,
    activeVersionId,
    tablePage,
    tableSize,
    searchFieldId,
    searchKeyword,
    fieldVisible,
    rowVisible,
    rowEditingId,
    excelInput,
    coverExcelInput,
    fieldForm,
    rowForm,
    activeVersion,
    isDraft,
    tableRows,
    tableTotal,
    fields,
    dataTableKey,
    loadDataset,
    selectVersion,
    loadDetail,
    backToList,
    addField,
    removeField,
    startFieldDrag,
    dropField,
    endFieldDrag,
    openFieldDialog,
    submitFields,
    openRowDialog,
    submitRow,
    removeRow,
    handleAddDataCommand,
    importExcel,
    coverExcel,
    publishDraft,
    removeVersion,
    coverDraft,
    formatTime
  }
}
