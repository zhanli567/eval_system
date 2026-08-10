import { onBeforeUnmount, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { taskApi } from '../../../api/task';
import { useResourceDescriptionEdit } from '../../../composables/useResourceDescriptionEdit';
import { formatDateTime } from '../../../utils/formatters';
import { movePreviousPageIfLastRow, sortParams, toggleDescSort } from '../../../utils/composableHelpers';
import { TASK_STATUS_OPTIONS, statusLabel } from '../../../utils/taskLabels';
import { formatTaskAppBinding } from '../../../utils/taskAppBinding';

const DELETABLE_STATUSES = ['pending', 'completed', 'failed', 'stopped'];
const STARTABLE_STATUSES = ['pending', 'failed', 'stopped'];
const STOPPABLE_STATUSES = ['running'];
const APP_TYPE_AGENT = 'agent';

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
            ...sortParams(state.sortBy, state.sortOrder)
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
    if (ctx.pollTimer !== undefined) {
        return;
    }
    ctx.pollTimer = window.setInterval(() => {
        if (!ctx.loading.value) {
            ctx.loadTasks({ silent: true });
        }
    }, 60000);
}

function stopListPolling(ctx) {
    if (ctx.pollTimer === undefined) {
        return;
    }
    window.clearInterval(ctx.pollTimer);
    ctx.pollTimer = undefined;
}

function canStartTask(row) {
    return STARTABLE_STATUSES.includes(row?.base?.status) && hasRunnableBinding(row);
}

function hasRunnableBinding(row) {
    const base = row?.base ?? {};
    return (base.appType === APP_TYPE_AGENT && Boolean(base.appId)) || Boolean(row?.evaluators?.length);
}

function canStopTask(row) {
    return STOPPABLE_STATUSES.includes(row.base.status);
}

function canDeleteTask(row) {
    return DELETABLE_STATUSES.includes(row.base.status);
}

function createTaskManagementActions(ctx, router) {
    async function loadTasks(options = {}) {
        await loadTaskPage(ctx.state, options);
    }
    return {
        loadTasks,
        ...createTaskQueryActions(ctx, loadTasks),
        ...createTaskRouteActions(router),
        ...createTaskRunActions(ctx, loadTasks),
        ...createTaskRemoveActions(ctx, loadTasks),
        ...createTaskPollingActions(ctx),
        ...createTaskStatusGuards()
    };
}

function createTaskQueryActions(ctx, loadTasks) {
    async function searchTasks() {
        ctx.state.page.value = 1;
        await loadTasks();
    }
    async function changeSize() {
        ctx.state.page.value = 1;
        await loadTasks();
    }
    function toggleSort(field) {
        toggleDescSort(ctx.state.sortBy, ctx.state.sortOrder, field);
        ctx.state.page.value = 1;
        loadTasks();
    }
    return { searchTasks, changeSize, toggleSort };
}

function createTaskRouteActions(router) {
    function openCreate() {
        router.push({ name: 'task-create' });
    }
    function openDetail(row) {
        router.push({ name: 'task-detail', params: { taskId: row.base.id } });
    }
    function copyTask(row) {
        router.push({ name: 'task-create', query: { copyFrom: row.base.id } });
    }
    return { openCreate, openDetail, copyTask };
}

function createTaskRunActions(ctx, loadTasks) {
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
    async function stopTask(row) {
        if (!canStopTask(row)) {
            ElMessage.warning('仅进行中的评测任务可以停止');
            return;
        }
        await ElMessageBox.confirm(
            '停止后将保留已完成结果，重新开始时仅继续未完成或失败的数据。确定停止吗？',
            '停止评测任务',
            { type: 'warning' }
        );
        setStopping(row.base.id, true);
        try {
            await taskApi.stopTask(row.base.id);
            ElMessage.success('评测任务已停止');
            await loadTasks();
        }
        finally {
            setStopping(row.base.id, false);
        }
    }
    function setStarting(taskId, value) {
        const next = new Set(ctx.startingTaskIds.value);
        value ? next.add(taskId) : next.delete(taskId);
        ctx.startingTaskIds.value = next;
    }
    function setStopping(taskId, value) {
        const next = new Set(ctx.stoppingTaskIds.value);
        value ? next.add(taskId) : next.delete(taskId);
        ctx.stoppingTaskIds.value = next;
    }
    function isStartingTask(taskId) {
        return ctx.startingTaskIds.value.has(taskId);
    }
    function isStoppingTask(taskId) {
        return ctx.stoppingTaskIds.value.has(taskId);
    }
    return { startTask, stopTask, isStartingTask, isStoppingTask };
}

function createTaskRemoveActions(ctx, loadTasks) {
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
    return { removeTask };
}

function createTaskPollingActions(ctx) {
    function startPolling() {
        startListPolling(ctx);
    }
    function stopPolling() {
        stopListPolling(ctx);
    }
    return { startPolling, stopPolling };
}

function createTaskStatusGuards() {
    return { canStartTask, canStopTask, canDeleteTask };
}

export function useTaskManagement() {
    const router = useRouter();
    const loading = ref(false);
    const tasks = ref([]);
    const startingTaskIds = ref(new Set());
    const stoppingTaskIds = ref(new Set());
    const total = ref(0);
    const page = ref(1);
    const size = ref(10);
    const keyword = ref('');
    const status = ref('');
    const sortBy = ref('');
    const sortOrder = ref('');
    const state = { loading, tasks, total, page, size, keyword, status, sortBy, sortOrder };
    const ctx = { state, loading, pollTimer: undefined, startingTaskIds, stoppingTaskIds };
    const actions = createTaskManagementActions(ctx, router);
    const descriptionEdit = useResourceDescriptionEdit({
        getId: (row) => row.base.id,
        getName: (row) => row.base.taskName,
        getDescription: (row) => row.base.description,
        update: taskApi.updateDescription,
        reload: actions.loadTasks
    });
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
        stoppingTaskIds,
        total,
        page,
        size,
        keyword,
        status,
        sortBy,
        sortOrder,
        statusOptions: TASK_STATUS_OPTIONS,
        ...descriptionEdit,
        loadTasks: actions.loadTasks,
        searchTasks: actions.searchTasks,
        changeSize: actions.changeSize,
        openCreate: actions.openCreate,
        openDetail: actions.openDetail,
        copyTask: actions.copyTask,
        startTask: actions.startTask,
        stopTask: actions.stopTask,
        isStartingTask: actions.isStartingTask,
        isStoppingTask: actions.isStoppingTask,
        removeTask: actions.removeTask,
        canStartTask: actions.canStartTask,
        canStopTask: actions.canStopTask,
        canDeleteTask: actions.canDeleteTask,
        toggleSort: actions.toggleSort,
        formatAppBinding: formatTaskAppBinding,
        statusLabel,
        formatTime
    };
}
