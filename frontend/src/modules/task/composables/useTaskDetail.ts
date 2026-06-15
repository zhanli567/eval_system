import { computed, onBeforeUnmount, ref, watch, type Ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { taskApi } from '../../../api/task'
import type { TaskDetail, TaskItemDetail } from '../../../types'

export function useTaskDetail(taskId: Ref<string>) {
  const router = useRouter()
  const loading = ref(false)
  const starting = ref(false)
  const detail = ref<TaskDetail>()
  const page = ref(1)
  const size = ref(8)
  let pollTimer: number | undefined

  const base = computed(() => detail.value?.base)
  const fields = computed(() => detail.value?.fields ?? [])
  const evaluators = computed(() => detail.value?.evaluators ?? [])
  const tags = computed(() => detail.value?.tags ?? [])
  const rows = computed(() => detail.value?.items.records ?? [])
  const total = computed(() => detail.value?.items.total ?? 0)

  watch(
    taskId,
    async () => {
      await loadDetail()
    },
    { immediate: true }
  )

  watch(
    () => base.value?.status,
    (status) => {
      if (status === 'running') {
        startPolling()
      } else {
        stopPolling()
      }
    }
  )

  onBeforeUnmount(stopPolling)

  async function loadDetail(options: { silent?: boolean } | number = {}) {
    if (!taskId.value) return
    const silent = typeof options === 'object' && Boolean(options.silent)
    if (!silent) {
      loading.value = true
    }
    try {
      detail.value = await taskApi.getTask(taskId.value, { page: page.value, size: size.value })
    } finally {
      if (!silent) {
        loading.value = false
      }
    }
  }

  function backToList() {
    router.push({ name: 'tasks' })
  }

  async function startTask() {
    if (!taskId.value) return
    starting.value = true
    try {
      detail.value = await taskApi.startTask(taskId.value)
      ElMessage.success('评测任务已开始')
      startPolling()
    } finally {
      starting.value = false
    }
  }

  function startPolling() {
    if (pollTimer !== undefined) return
    pollTimer = window.setInterval(() => {
      if (!loading.value && !starting.value) {
        loadDetail({ silent: true })
      }
    }, 3000)
  }

  function stopPolling() {
    if (pollTimer === undefined) return
    window.clearInterval(pollTimer)
    pollTimer = undefined
  }

  function openAnnotation(row: TaskItemDetail) {
    router.push({ name: 'task-annotation', params: { taskId: taskId.value, taskItemId: row.id } })
  }

  function statusLabel(value?: string) {
    const map: Record<string, string> = {
      pending: '待执行',
      running: '进行中',
      completed: '评测完成',
      failed: '评测失败',
      annotation_pending: '待标注',
      annotating: '标注中',
      skipped: '跳过'
    }
    return value ? map[value] || value : '-'
  }

  function statusTagType(value?: string) {
    if (value === 'completed') return 'success'
    if (value === 'running' || value === 'annotation_pending') return 'primary'
    if (value === 'failed') return 'danger'
    return 'info'
  }

  function passTagType(value?: string) {
    if (value === 'pass') return 'success'
    if (value === 'fail') return 'danger'
    return 'info'
  }

  function formatRate(value?: number) {
    return value === undefined || value === null ? '-' : `${value}%`
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

  function formatTime(value?: string) {
    if (!value) return '-'
    const numberValue = Number(value)
    if (Number.isNaN(numberValue)) return value
    return new Date(numberValue).toLocaleString()
  }

  return {
    loading,
    starting,
    detail,
    page,
    size,
    base,
    fields,
    evaluators,
    tags,
    rows,
    total,
    loadDetail,
    backToList,
    startTask,
    openAnnotation,
    statusLabel,
    statusTagType,
    passTagType,
    formatRate,
    tagTypeLabel,
    formatTime
  }
}
