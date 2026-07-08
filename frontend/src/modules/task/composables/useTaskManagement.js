import { onBeforeUnmount, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { taskApi } from '../../../api/task';
import { formatDateTime } from '../../../utils/formatters';
import { useColumnWidths } from '../../../utils/tableColumns';
export function useTaskManagement() {
    const router = useRouter();
    const loading = ref(false);
    const tasks = ref([]);
    const startingTaskIds = ref(new Set());
    const total = ref(0);
    const page = ref(1);
    const size = ref(10);
    const keyword = ref('');
    const status = ref('');
    const sortBy = ref('lastUpdatedDate');
    const sortOrder = ref('desc');
    let pollTimer;
    const statusOptions = [
        { label: '全部状态', value: '' },
        { label: '待执行', value: 'pending' },
        { label: '进行中', value: 'running' },
        { label: '评测完成', value: 'completed' },
        { label: '评测失败', value: 'failed' }
    ];
    const { columnWidths, handleColumnResize } = useColumnWidths({
        status: { width: 90, min: 76, max: 120 },
        taskName: { width: 220, min: 160, max: 380 },
        datasetName: { width: 210, min: 160, max: 360 },
        app: { width: 260, min: 180, max: 460 },
        description: { width: 260, min: 180, max: 500 },
        evaluators: { width: 220, min: 160, max: 360 },
        tags: { width: 190, min: 140, max: 320 },
        createdByName: { width: 140, min: 100, max: 220 },
        createdDate: { width: 190, min: 160, max: 240 },
        actions: { width: 190, min: 160, max: 230 }
    });
    onMounted(async () => {
        await loadTasks();
        startPolling();
    });
    onBeforeUnmount(stopPolling);
    async function loadTasks(options = {}) {
        if (!options.silent) {
            loading.value = true;
        }
        try {
            const result = await taskApi.listTasks({
                page: page.value,
                size: size.value,
                keyword: keyword.value,
                status: status.value,
                sortBy: sortBy.value,
                sortOrder: sortOrder.value
            });
            tasks.value = result.records;
            total.value = result.total;
        }
        finally {
            if (!options.silent) {
                loading.value = false;
            }
        }
    }
    function startPolling() {
        if (pollTimer !== undefined)
            return;
        pollTimer = window.setInterval(() => {
            if (!loading.value) {
                loadTasks({ silent: true });
            }
        }, 3000);
    }
    function stopPolling() {
        if (pollTimer === undefined)
            return;
        window.clearInterval(pollTimer);
        pollTimer = undefined;
    }
    async function searchTasks() {
        page.value = 1;
        await loadTasks();
    }
    async function changeSize() {
        page.value = 1;
        await loadTasks();
    }
    function openCreate() {
        router.push({ name: 'task-create' });
    }
    function openDetail(row) {
        router.push({ name: 'task-detail', params: { taskId: row.base.id } });
    }
    async function startTask(row) {
        setStarting(row.base.id, true);
        try {
            await taskApi.startTask(row.base.id);
            ElMessage.success('评测任务已开始');
            await loadTasks();
        }
        finally {
            setStarting(row.base.id, false);
        }
    }
    function setStarting(taskId, value) {
        const next = new Set(startingTaskIds.value);
        if (value) {
            next.add(taskId);
        }
        else {
            next.delete(taskId);
        }
        startingTaskIds.value = next;
    }
    function isStartingTask(taskId) {
        return startingTaskIds.value.has(taskId);
    }
    async function removeTask(row) {
        if (!canDeleteTask(row)) {
            ElMessage.warning('仅待执行、评测完成和评测失败的任务可删除');
            return;
        }
        await ElMessageBox.confirm(`确定删除评测任务“${row.base.taskName}”吗？`, '删除评测任务', { type: 'warning' });
        await taskApi.deleteTask(row.base.id);
        ElMessage.success('已删除');
        if (tasks.value.length === 1 && page.value > 1) {
            page.value -= 1;
        }
        await loadTasks();
    }
    function toggleSort(field) {
        if (sortBy.value === field) {
            sortOrder.value = sortOrder.value === 'desc' ? 'asc' : 'desc';
        }
        else {
            sortBy.value = field;
            sortOrder.value = 'desc';
        }
        page.value = 1;
        loadTasks();
    }
    function statusLabel(value) {
        return statusOptions.find((item) => item.value === value)?.label || value || '-';
    }
    function canStartTask(row) {
        return row.base.status === 'pending' || row.base.status === 'failed';
    }
    function canDeleteTask(row) {
        return row.base.status === 'pending' || row.base.status === 'completed' || row.base.status === 'failed';
    }
    const formatTime = formatDateTime;
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
        columnWidths,
        statusOptions,
        loadTasks,
        searchTasks,
        changeSize,
        openCreate,
        openDetail,
        startTask,
        isStartingTask,
        removeTask,
        canStartTask,
        canDeleteTask,
        toggleSort,
        handleColumnResize,
        statusLabel,
        formatTime
    };
}
