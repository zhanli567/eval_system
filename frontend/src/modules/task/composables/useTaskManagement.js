import { onBeforeUnmount, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { taskApi } from '../../../api/task';
import { formatDateTime } from '../../../utils/formatters';
import { movePreviousPageIfLastRow, toggleDescSort } from '../../../utils/composableHelpers';
import { TASK_STATUS_OPTIONS, statusLabel } from '../../../utils/taskLabels';
import { useColumnWidths } from '../../../utils/tableColumns';

const DELETABLE_STATUSES = ['pending', 'completed', 'failed'];
const STARTABLE_STATUSES = ['pending', 'failed'];

function taskColumns() {
    return useColumnWidths({
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
}

async function loadTaskPage(state, options = {}) {
    if (!options.silent) {
        state.loading.value = true;
    }
    try {
        const result = await taskApi.listTasks({
            page: state.page.value,
            size: state.size.value,
            keyword: state.keyword.value,
            status: state.status.value,
            sortBy: state.sortBy.value,
            sortOrder: state.sortOrder.value
        });
        state.tasks.value = result.records;
        state.total.value = result.total;
    }
    finally {
        if (!options.silent) {
            state.loading.value = false;
        }
    }
}

function startListPolling(ctx) {
    if (ctx.pollTimer !== undefined)
        return;
    ctx.pollTimer = window.setInterval(() => {
        if (!ctx.loading.value) {
            ctx.loadTasks({ silent: true });
        }
    }, 3000);
}

function stopListPolling(ctx) {
    if (ctx.pollTimer === undefined)
        return;
    window.clearInterval(ctx.pollTimer);
    ctx.pollTimer = undefined;
}

function createTaskManagementActions(ctx, router) {
    async function loadTasks(options = {}) {
        await loadTaskPage(ctx.state, options);
    }
    async function searchTasks() {
        ctx.state.page.value = 1;
        await loadTasks();
    }
    async function changeSize() {
        ctx.state.page.value = 1;
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
        const next = new Set(ctx.startingTaskIds.value);
        value ? next.add(taskId) : next.delete(taskId);
        ctx.startingTaskIds.value = next;
    }
    function isStartingTask(taskId) {
        return ctx.startingTaskIds.value.has(taskId);
    }
    async function removeTask(row) {
        if (!canDeleteTask(row)) {
            ElMessage.warning('仅待执行、评测完成和评测失败的任务可删除');
            return;
        }
        await ElMessageBox.confirm(`确定删除评测任务“${row.base.taskName}”吗？`, '删除评测任务', { type: 'warning' });
        await taskApi.deleteTask(row.base.id);
        ElMessage.success('已删除');
        movePreviousPageIfLastRow(ctx.state.tasks, ctx.state.page);
        await loadTasks();
    }
    function toggleSort(field) {
        toggleDescSort(ctx.state.sortBy, ctx.state.sortOrder, field);
        ctx.state.page.value = 1;
        loadTasks();
    }
    function startPolling() {
        startListPolling(ctx);
    }
    function stopPolling() {
        stopListPolling(ctx);
    }
    function canStartTask(row) {
        return STARTABLE_STATUSES.includes(row.base.status);
    }
    function canDeleteTask(row) {
        return DELETABLE_STATUSES.includes(row.base.status);
    }
    return { loadTasks, searchTasks, changeSize, openCreate, openDetail, startTask, isStartingTask, removeTask, canStartTask, canDeleteTask, toggleSort, startPolling, stopPolling };
}

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
    const columns = taskColumns();
    const state = { loading, tasks, total, page, size, keyword, status, sortBy, sortOrder };
    const ctx = { state, loading, pollTimer: undefined, startingTaskIds };
    const actions = createTaskManagementActions(ctx, router);
    ctx.loadTasks = actions.loadTasks;
    onMounted(async () => {
        await actions.loadTasks();
        actions.startPolling();
    });
    onBeforeUnmount(actions.stopPolling);

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
        columnWidths: columns.columnWidths,
        statusOptions: TASK_STATUS_OPTIONS,
        loadTasks: actions.loadTasks,
        searchTasks: actions.searchTasks,
        changeSize: actions.changeSize,
        openCreate: actions.openCreate,
        openDetail: actions.openDetail,
        startTask: actions.startTask,
        isStartingTask: actions.isStartingTask,
        removeTask: actions.removeTask,
        canStartTask: actions.canStartTask,
        canDeleteTask: actions.canDeleteTask,
        toggleSort: actions.toggleSort,
        handleColumnResize: columns.handleColumnResize,
        statusLabel,
        formatTime
    };
}
