import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { taskApi } from '../../../api/task'
import type { TaskStatus, TaskSummary } from '../../../types'

export function useTaskManagement() {
  const router = useRouter()
  const loading = ref(false)
  const tasks = ref<TaskSummary[]>([])
  const startingTaskIds = ref<Set<string>>(new Set())
  const total = ref(0)
  const page = ref(1)
  const size = ref(8)
  const keyword = ref('')
  const status = ref<TaskStatus | ''>('')
  const sortBy = ref<'createdAt' | 'updatedAt'>('updatedAt')
  const sortOrder = ref<'asc' | 'desc'>('desc')

  const statusOptions = [
    { label: '全部状态', value: '' },
    { label: '待执行', value: 'pending' },
    { label: '进行中', value: 'running' },
    { label: '评测完成', value: 'completed' },
    { label: '评测终止', value: 'terminated' },
    { label: '评测失败', value: 'failed' }
  ] as const

  onMounted(loadTasks)

  async function loadTasks() {
    loading.value = true
    try {
      const result = await taskApi.listTasks({
        page: page.value,
        size: size.value,
        keyword: keyword.value,
        status: status.value,
        sortBy: sortBy.value,
        sortOrder: sortOrder.value
      })
      tasks.value = result.records
      total.value = result.total
    } finally {
      loading.value = false
    }
  }

  async function searchTasks() {
    page.value = 1
    await loadTasks()
  }

  function openCreate() {
    router.push({ name: 'task-create' })
  }

  function openDetail(row: TaskSummary) {
    router.push({ name: 'task-detail', params: { taskId: row.base.id } })
  }

  async function startTask(row: TaskSummary) {
    setStarting(row.base.id, true)
    try {
      await taskApi.startTask(row.base.id)
      ElMessage.success('评测任务已开始')
      await loadTasks()
    } finally {
      setStarting(row.base.id, false)
    }
  }

  function setStarting(taskId: string, value: boolean) {
    const next = new Set(startingTaskIds.value)
    if (value) {
      next.add(taskId)
    } else {
      next.delete(taskId)
    }
    startingTaskIds.value = next
  }

  function isStartingTask(taskId: string) {
    return startingTaskIds.value.has(taskId)
  }

  async function terminateTask(row: TaskSummary) {
    await ElMessageBox.confirm(`确定终止评测任务“${row.base.taskName}”吗？`, '终止评测任务', { type: 'warning' })
    await taskApi.terminateTask(row.base.id)
    ElMessage.success('评测任务已终止')
    await loadTasks()
  }

  async function removeTask(row: TaskSummary) {
    await ElMessageBox.confirm(`确定删除评测任务“${row.base.taskName}”吗？`, '删除评测任务', { type: 'warning' })
    await taskApi.deleteTask(row.base.id)
    ElMessage.success('已删除')
    await loadTasks()
  }

  function toggleSort(field: 'createdAt' | 'updatedAt') {
    if (sortBy.value === field) {
      sortOrder.value = sortOrder.value === 'desc' ? 'asc' : 'desc'
    } else {
      sortBy.value = field
      sortOrder.value = 'desc'
    }
    loadTasks()
  }

  function statusLabel(value?: string) {
    return statusOptions.find((item) => item.value === value)?.label || value || '-'
  }

  function statusTagType(value?: string) {
    if (value === 'completed') return 'success'
    if (value === 'running') return 'primary'
    if (value === 'failed') return 'danger'
    if (value === 'terminated') return 'warning'
    return 'info'
  }

  function dimensionStatusLabel(value?: string) {
    if (value === 'completed') return '完成'
    if (value === 'running') return '进行中'
    if (value === 'annotating') return '标注中'
    if (value === 'failed') return '失败'
    return '待处理'
  }

  function formatRate(value?: number) {
    return value === undefined || value === null ? '-' : `${value}%`
  }

  function formatTime(value?: string) {
    if (!value) return '-'
    const numberValue = Number(value)
    if (Number.isNaN(numberValue)) return value
    return new Date(numberValue).toLocaleString()
  }

  return {
    loading,
    tasks,
    startingTaskIds,
    total,
    page,
    size,
    keyword,
    status,
    sortBy,
    sortOrder,
    statusOptions,
    loadTasks,
    searchTasks,
    openCreate,
    openDetail,
    startTask,
    isStartingTask,
    terminateTask,
    removeTask,
    toggleSort,
    statusLabel,
    statusTagType,
    dimensionStatusLabel,
    formatRate,
    formatTime
  }
}
