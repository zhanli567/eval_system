import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { evaluatorApi } from '../../../api/evaluator'
import type {
  EvaluatorSummary,
  EvaluatorType,
  PresetCategory,
  PresetEvaluatorDetail,
  PresetEvaluatorSummary
} from '../../../types'

export function useEvaluatorManagement() {
  const router = useRouter()
  const activeTab = ref<'custom' | 'preset'>('custom')

  const customLoading = ref(false)
  const customEvaluators = ref<EvaluatorSummary[]>([])
  const customTotal = ref(0)
  const customPage = ref(1)
  const customSize = ref(8)
  const customKeyword = ref('')
  const customType = ref<EvaluatorType | ''>('')

  const categories = ref<PresetCategory[]>([])
  const activeCategoryId = ref('')
  const presetLoading = ref(false)
  const presetEvaluators = ref<PresetEvaluatorSummary[]>([])
  const presetTotal = ref(0)
  const presetPage = ref(1)
  const presetSize = ref(12)
  const presetKeyword = ref('')

  const pickerVisible = ref(false)
  const pickerCategoryId = ref('')
  const pickerKeyword = ref('')
  const pickerPage = ref(1)
  const pickerSize = ref(12)
  const pickerTotal = ref(0)
  const pickerLoading = ref(false)
  const pickerPresets = ref<PresetEvaluatorSummary[]>([])

  const detailVisible = ref(false)
  const detailLoading = ref(false)
  const selectedPreset = ref<PresetEvaluatorDetail | null>(null)

  const categoryOptions = computed(() => [
    { id: '', categoryName: '全部分类', displayOrder: 0 },
    ...categories.value
  ])

  onMounted(async () => {
    await Promise.all([loadCategories(), loadCustomEvaluators()])
    await loadPresetEvaluators()
  })

  async function loadCategories() {
    categories.value = await evaluatorApi.listPresetCategories()
  }

  async function loadCustomEvaluators() {
    customLoading.value = true
    try {
      const page = await evaluatorApi.listEvaluators({
        page: customPage.value,
        size: customSize.value,
        evaluatorType: customType.value,
        keyword: customKeyword.value
      })
      customEvaluators.value = page.records
      customTotal.value = page.total
    } finally {
      customLoading.value = false
    }
  }

  async function searchCustom() {
    customPage.value = 1
    await loadCustomEvaluators()
  }

  async function loadPresetEvaluators() {
    presetLoading.value = true
    try {
      const page = await evaluatorApi.listPresetEvaluators({
        page: presetPage.value,
        size: presetSize.value,
        categoryId: activeCategoryId.value,
        keyword: presetKeyword.value
      })
      presetEvaluators.value = page.records
      presetTotal.value = page.total
    } finally {
      presetLoading.value = false
    }
  }

  async function searchPreset() {
    presetPage.value = 1
    await loadPresetEvaluators()
  }

  async function selectPresetCategory(categoryId: string) {
    activeCategoryId.value = categoryId
    presetPage.value = 1
    await loadPresetEvaluators()
  }

  async function openPicker() {
    pickerCategoryId.value = ''
    pickerKeyword.value = ''
    pickerPage.value = 1
    pickerVisible.value = true
    await loadPickerPresets()
  }

  async function loadPickerPresets() {
    pickerLoading.value = true
    try {
      const page = await evaluatorApi.listPresetEvaluators({
        page: pickerPage.value,
        size: pickerSize.value,
        categoryId: pickerCategoryId.value,
        keyword: pickerKeyword.value
      })
      pickerPresets.value = page.records
      pickerTotal.value = page.total
    } finally {
      pickerLoading.value = false
    }
  }

  async function searchPicker() {
    pickerPage.value = 1
    await loadPickerPresets()
  }

  async function selectPickerCategory(categoryId: string) {
    pickerCategoryId.value = categoryId
    pickerPage.value = 1
    await loadPickerPresets()
  }

  async function viewPreset(presetId: string) {
    detailVisible.value = true
    detailLoading.value = true
    try {
      selectedPreset.value = await evaluatorApi.getPresetEvaluator(presetId)
    } finally {
      detailLoading.value = false
    }
  }

  function createCustom() {
    pickerVisible.value = false
    router.push({ name: 'evaluator-create' })
  }

  function createFromPreset(presetId: string) {
    pickerVisible.value = false
    detailVisible.value = false
    router.push({ name: 'evaluator-create', query: { presetId } })
  }

  function editEvaluator(row: EvaluatorSummary) {
    router.push({ name: 'evaluator-edit', params: { evaluatorId: row.id } })
  }

  async function copyEvaluator(row: EvaluatorSummary) {
    const copied = await evaluatorApi.copyEvaluator(row.id)
    ElMessage.success('评估器已复制')
    await router.push({ name: 'evaluator-edit', params: { evaluatorId: copied.evaluatorId } })
  }

  async function removeEvaluator(row: EvaluatorSummary) {
    await ElMessageBox.confirm(`确定删除评估器“${row.evaluatorName}”吗？`, '删除评估器', { type: 'warning' })
    await evaluatorApi.deleteEvaluator(row.id)
    ElMessage.success('已删除')
    await loadCustomEvaluators()
  }

  function typeLabel(type: EvaluatorType | string) {
    return type === 'code' ? 'Code' : 'LLM'
  }

  function formatTime(value?: string) {
    if (!value) return '-'
    const numberValue = Number(value)
    if (Number.isNaN(numberValue)) return value
    return new Date(numberValue).toLocaleString()
  }

  return {
    activeTab,
    customLoading,
    customEvaluators,
    customTotal,
    customPage,
    customSize,
    customKeyword,
    customType,
    categories,
    categoryOptions,
    activeCategoryId,
    presetLoading,
    presetEvaluators,
    presetTotal,
    presetPage,
    presetSize,
    presetKeyword,
    pickerVisible,
    pickerCategoryId,
    pickerKeyword,
    pickerPage,
    pickerSize,
    pickerTotal,
    pickerLoading,
    pickerPresets,
    detailVisible,
    detailLoading,
    selectedPreset,
    loadCustomEvaluators,
    searchCustom,
    loadPresetEvaluators,
    searchPreset,
    selectPresetCategory,
    openPicker,
    loadPickerPresets,
    searchPicker,
    selectPickerCategory,
    viewPreset,
    createCustom,
    createFromPreset,
    editEvaluator,
    copyEvaluator,
    removeEvaluator,
    typeLabel,
    formatTime
  }
}
