import { computed, reactive, ref, watch, type Ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { taskApi } from '../../../api/task'
import type { AnnotationDetail, TaskTagAnnotation } from '../../../types'

export function useTaskAnnotation(taskId: Ref<string>, taskItemId: Ref<string>) {
  const router = useRouter()
  const loading = ref(false)
  const saving = ref(false)
  const loadError = ref('')
  const detail = ref<AnnotationDetail>()
  const form = reactive<Record<string, string | number | undefined>>({})

  const task = computed(() => detail.value?.task)
  const item = computed(() => detail.value?.item)
  const fields = computed(() => detail.value?.fields ?? [])
  const tags = computed(() => detail.value?.tags ?? [])
  const evaluators = computed(() => detail.value?.evaluators ?? [])
  const previousItemId = computed(() => detail.value?.previousItemId || '')
  const nextItemId = computed(() => detail.value?.nextItemId || '')

  watch(
    () => [taskId.value, taskItemId.value] as const,
    async () => {
      await loadAnnotation()
    },
    { immediate: true }
  )

  async function loadAnnotation() {
    if (!taskId.value || !taskItemId.value) return
    loading.value = true
    loadError.value = ''
    try {
      detail.value = await taskApi.getAnnotation(taskId.value, taskItemId.value)
      fillForm()
    } catch (error) {
      detail.value = undefined
      loadError.value = error instanceof Error ? error.message : '加载标注数据失败'
      ElMessage.error(loadError.value)
    } finally {
      loading.value = false
    }
  }

  function fillForm() {
    for (const key of Object.keys(form)) {
      delete form[key]
    }
    for (const tag of tags.value) {
      if (tag.tagType === 'number') {
        form[tag.taskTagId] = tag.result?.valueNumber
      } else if (tag.tagType === 'category' || tag.tagType === 'boolean') {
        form[tag.taskTagId] = tag.result?.tagOptionId || ''
      } else {
        form[tag.taskTagId] = tag.result?.valueText || ''
      }
    }
  }

  async function saveAnnotation() {
    if (!taskId.value || !taskItemId.value) return
    if (!validate()) return
    saving.value = true
    try {
      detail.value = await taskApi.saveAnnotation(taskId.value, taskItemId.value, {
        tags: tags.value.map((tag) => {
          const value = form[tag.taskTagId]
          return {
            taskTagId: tag.taskTagId,
            valueText: tag.tagType === 'text' ? String(value ?? '') : undefined,
            valueNumber: tag.tagType === 'number' && value !== undefined && value !== '' ? Number(value) : undefined,
            tagOptionId: tag.tagType === 'category' || tag.tagType === 'boolean' ? String(value ?? '') : undefined
          }
        })
      })
      fillForm()
      ElMessage.success('标注已保存')
    } catch (error) {
      ElMessage.error(error instanceof Error ? error.message : '保存标注失败')
    } finally {
      saving.value = false
    }
  }

  function validate() {
    for (const tag of tags.value) {
      const value = form[tag.taskTagId]
      if (tag.tagType === 'number') {
        if (value === undefined || value === '') {
          ElMessage.warning(`请输入${tag.tagName}`)
          return false
        }
      } else if (!String(value ?? '').trim()) {
        ElMessage.warning(`请完成${tag.tagName}`)
        return false
      }
    }
    return true
  }

  function backToDetail() {
    router.push({ name: 'task-detail', params: { taskId: taskId.value } })
  }

  function goItem(targetItemId: string) {
    if (!targetItemId) return
    router.push({ name: 'task-annotation', params: { taskId: taskId.value, taskItemId: targetItemId } })
  }

  function passTagType(value?: string) {
    if (value === 'pass') return 'success'
    if (value === 'fail') return 'danger'
    return 'info'
  }

  function tagTypeLabel(value?: string) {
    const map: Record<string, string> = {
      category: '分类',
      boolean: '布尔',
      number: '数字',
      text: '文本'
    }
    return value ? map[value] || value : '-'
  }

  function optionLabel(tag: TaskTagAnnotation) {
    if (tag.tagType === 'number') {
      return `范围 ${tag.minValue ?? '-'}-${tag.maxValue ?? '-'}，通过阈值 ${tag.passThreshold ?? '-'}`
    }
    return tag.description || '暂无描述'
  }

  function appOutputEmptyDescription() {
    if (!task.value) {
      return '标注数据加载后展示应用输出'
    }
    if (task.value.appType !== 'agent') {
      return '当前任务未关联应用，无应用输出'
    }
    if (item.value?.appOutputStatus === 'failed') {
      return '应用调用失败，暂无应用输出'
    }
    if (item.value?.appOutputStatus === 'pending') {
      return '应用输出待生成'
    }
    return '暂无应用输出'
  }

  return {
    loading,
    saving,
    loadError,
    detail,
    form,
    task,
    item,
    fields,
    tags,
    evaluators,
    previousItemId,
    nextItemId,
    loadAnnotation,
    saveAnnotation,
    backToDetail,
    goItem,
    passTagType,
    tagTypeLabel,
    optionLabel,
    appOutputEmptyDescription
  }
}
