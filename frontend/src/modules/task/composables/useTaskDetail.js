import { computed, onBeforeUnmount, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { taskApi } from '../../../api/task';
import { formatDateTime } from '../../../utils/formatters';
import { passTagType, statusLabel, tagTypeLabel } from '../../../utils/taskLabels';
import { useTaskAppDisplay } from './useTaskAppDisplay';

async function loadTaskDetail(ctx, options = {}) {
    if (!ctx.taskId.value)
        return;
    const silent = Boolean(options.silent);
    if (!silent) {
        ctx.loading.value = true;
    }
    try {
        ctx.detail.value = await taskApi.getTask(ctx.taskId.value, { page: ctx.page.value, size: ctx.size.value });
        void ctx.appDisplay.load([ctx.detail.value?.base]);
    }
    finally {
        if (!silent) {
            ctx.loading.value = false;
        }
    }
}

function startTaskPolling(ctx) {
    if (ctx.pollTimer !== undefined)
        return;
    ctx.pollTimer = window.setInterval(() => {
        if (!ctx.loading.value && !ctx.starting.value) {
            ctx.loadDetail({ silent: true });
        }
    }, 3000);
}

function stopTaskPolling(ctx) {
    if (ctx.pollTimer === undefined)
        return;
    window.clearInterval(ctx.pollTimer);
    ctx.pollTimer = undefined;
}

function createTaskDetailActions(ctx, router) {
    async function loadDetail(options = {}) {
        await loadTaskDetail(ctx, options);
    }
    async function changeSize() {
        ctx.page.value = 1;
        await loadDetail();
    }
    function backToList() {
        router.push({ name: 'tasks' });
    }
    async function startTask() {
        if (!ctx.taskId.value)
            return;
        ctx.starting.value = true;
        try {
            ctx.detail.value = await taskApi.startTask(ctx.taskId.value);
            ElMessage.success('评测任务已开始');
            startPolling();
        }
        finally {
            ctx.starting.value = false;
        }
    }
    function startPolling() {
        startTaskPolling(ctx);
    }
    function stopPolling() {
        stopTaskPolling(ctx);
    }
    function openAnnotation(row) {
        router.push({ name: 'task-annotation', params: { taskId: ctx.taskId.value, taskItemId: row.id } });
    }
    return { loadDetail, changeSize, backToList, startTask, startPolling, stopPolling, openAnnotation };
}

const taskBase = (detail) => detail?.base;
const taskFields = (detail) => detail?.fields ?? [];
const taskEvaluators = (detail) => detail?.evaluators ?? [];
const taskTags = (detail) => detail?.tags ?? [];
const taskRows = (detail) => detail?.items.records ?? [];
const taskTotal = (detail) => detail?.items.total ?? 0;

function syncPollingByStatus(status, actions) {
    status === 'running' ? actions.startPolling() : actions.stopPolling();
}

export function useTaskDetail(taskId) {
    const router = useRouter();
    const loading = ref(false);
    const starting = ref(false);
    const detail = ref();
    const page = ref(1);
    const size = ref(10);
    const appDisplay = useTaskAppDisplay();
    const ctx = { taskId, loading, starting, detail, page, size, pollTimer: undefined, appDisplay };
    const actions = createTaskDetailActions(ctx, router);
    ctx.loadDetail = actions.loadDetail;
    const base = computed(() => taskBase(detail.value));
    const fields = computed(() => taskFields(detail.value));
    const evaluators = computed(() => taskEvaluators(detail.value));
    const tags = computed(() => taskTags(detail.value));
    const rows = computed(() => taskRows(detail.value));
    const total = computed(() => taskTotal(detail.value));
    watch(taskId, async () => {
        await actions.loadDetail();
    }, { immediate: true });
    watch(() => base.value?.status, (status) => {
        syncPollingByStatus(status, actions);
    });
    onBeforeUnmount(actions.stopPolling);

    const formatTime = formatDateTime;
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
        loadDetail: actions.loadDetail,
        backToList: actions.backToList,
        startTask: actions.startTask,
        openAnnotation: actions.openAnnotation,
        changeSize: actions.changeSize,
        formatAppBinding: appDisplay.format,
        statusLabel,
        passTagType,
        tagTypeLabel,
        formatTime
    };
}
