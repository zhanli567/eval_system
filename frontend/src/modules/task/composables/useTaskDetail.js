import { computed, onBeforeUnmount, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { tagApi } from '../../../api/tag';
import { taskApi } from '../../../api/task';
import { formatDateTime } from '../../../utils/formatters';
import { passTagType, statusLabel, tagTypeLabel } from '../../../utils/taskLabels';
import { formatTaskAppBinding } from '../../../utils/taskAppBinding';

const STOPPABLE_STATUSES = ['running'];
const TASK_TAG_TYPE_OPTIONS = [
    { value: 'category', label: '分类' },
    { value: 'boolean', label: '布尔' },
    { value: 'number', label: '数字' },
    { value: 'text', label: '文本' }
];

async function loadTaskDetail(ctx, options = {}) {
    if (!ctx.taskId.value) {
        return;
    }
    const silent = Boolean(options.silent);
    if (!silent) {
        ctx.loading.value = true;
    }
    try {
        ctx.detail.value = await taskApi.getTask(ctx.taskId.value, { page: ctx.page.value, size: ctx.size.value });
    } finally {
        if (!silent) {
            ctx.loading.value = false;
        }
    }
}

function startTaskPolling(ctx) {
    if (ctx.pollTimer !== undefined) {
        return;
    }
    ctx.pollTimer = window.setInterval(() => {
        if (!ctx.loading.value) {
            ctx.loadDetail({ silent: true });
        }
    }, 60000);
}

function stopTaskPolling(ctx) {
    if (ctx.pollTimer === undefined) {
        return;
    }
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
    async function stopTask() {
        if (!ctx.taskId.value) {
            return;
        }
        await ElMessageBox.confirm(
            '停止后将保留已完成结果，未执行或未完成的数据会标记为已中止。已中止任务可重新开始，重新开始会从头重跑。确定停止吗？',
            '停止评测任务',
            { type: 'warning' }
        );
        ctx.stopping.value = true;
        try {
            ctx.detail.value = await taskApi.stopTask(ctx.taskId.value);
            ElMessage.success('评测任务已停止');
            stopPolling();
        } finally {
            ctx.stopping.value = false;
        }
    }
    function startPolling() {
        startTaskPolling(ctx);
    }
    function stopPolling() {
        stopTaskPolling(ctx);
    }
    function openAnnotation(row) {
        const mode = canAnnotateItem(ctx, row) ? 'annotate' : 'detail';
        router.push({
            name: 'task-annotation',
            params: { taskId: ctx.taskId.value, taskItemId: row.id },
            query: { mode }
        });
    }
    return { loadDetail, changeSize, backToList, stopTask, startPolling, stopPolling, openAnnotation };
}

function createTagActions(ctx) {
    async function loadAllTags() {
        await loadPagedTags(ctx);
    }
    async function openTagDrawer() {
        ctx.tagDrawerVisible.value = true;
        await loadAllTags();
    }
    async function addTaskTag(tag) {
        if (!tag?.id) {
            return;
        }
        await runTagOperation(ctx, tag.id, async () => {
            await taskApi.addTaskTag(ctx.taskId.value, tag.id);
            ElMessage.success('标签已添加');
            await refreshTaskAndTags(ctx, loadAllTags);
        });
    }
    async function removeTaskTagByTag(tag) {
        const binding = findTaskTagByTagId(ctx.detail.value?.tags ?? [], tag?.id);
        if (!binding) {
            return;
        }
        await removeTaskTag(binding, tag.id);
    }
    async function removeTaskTag(tag, operatingTagId = tag?.tagId || tag?.taskTagId) {
        if (!tag?.taskTagId) {
            return;
        }
        await runTagOperation(ctx, operatingTagId, async () => {
            await confirmTaskTagRemove(tag);
            await taskApi.deleteTaskTag(ctx.taskId.value, tag.taskTagId);
            ElMessage.success('标签已移除');
            await refreshTaskAndTags(ctx, loadAllTags);
        });
    }
    async function searchAllTags() {
        ctx.tagPage.value = 1;
        await loadAllTags();
    }
    async function changeTagSize() {
        ctx.tagPage.value = 1;
        await loadAllTags();
    }
    return { loadAllTags, openTagDrawer, addTaskTag, removeTaskTagByTag, removeTaskTag, searchAllTags, changeTagSize };
}

async function runTagOperation(ctx, tagId, operation) {
    if (!tagId || ctx.tagOperatingIds.value.includes(tagId)) {
        return;
    }
    ctx.tagOperatingIds.value = [...ctx.tagOperatingIds.value, tagId];
    try {
        await operation();
    } finally {
        ctx.tagOperatingIds.value = ctx.tagOperatingIds.value.filter((item) => item !== tagId);
    }
}

async function loadPagedTags(ctx) {
    ctx.tagLoading.value = true;
    try {
        const page = await tagApi.listTags(tagPageParams(ctx));
        ctx.allTags.value = page.records;
        ctx.tagTotal.value = page.total;
    } finally {
        ctx.tagLoading.value = false;
    }
}

function tagPageParams(ctx) {
    return {
        page: ctx.tagPage.value,
        size: ctx.tagSize.value,
        tagType: ctx.tagTypeFilter.value,
        keyword: ctx.tagKeyword.value
    };
}

async function refreshTaskAndTags(ctx, loadAllTags) {
    await ctx.loadDetail();
    await loadAllTags();
}

function confirmTaskTagRemove(tag) {
    return ElMessageBox.confirm(
        '请注意，删除后，所有已经录入的该标签均会删除',
        `是否确认删除标签：${tag?.tagName || '-'}`,
        {
            type: 'warning',
            confirmButtonText: '删除',
            cancelButtonText: '取消',
            confirmButtonClass: 'el-button--danger'
        }
    );
}

function findTaskTagByTagId(tags, tagId) {
    return tags.find((item) => item.tagId === tagId);
}

function hasTagBindings(ctx) {
    return Boolean(ctx.detail.value?.tags?.length);
}

function isStoppedForAnnotation(ctx, row) {
    return ctx.detail.value?.base?.status === 'stopped' || row?.status === 'stopped';
}

function canAnnotateItem(ctx, row) {
    return hasTagBindings(ctx) && !isStoppedForAnnotation(ctx, row);
}

const taskBase = (detail) => detail?.base;
const taskFields = (detail) => detail?.fields ?? [];
const taskEvaluators = (detail) => detail?.evaluators ?? [];
const taskTags = (detail) => detail?.tags ?? [];
const taskRows = (detail) => detail?.items.records ?? [];
const taskTotal = (detail) => detail?.items.total ?? 0;

function syncPollingByStatus(status, actions) {
    if (status === 'running') {
        actions.startPolling();
    } else {
        actions.stopPolling();
    }
}

function createDefaultColumnSettings(detail) {
    return [
        ...taskFields(detail).map((field) => tableColumn('field', field.id, field.fieldName || '评测集字段', datasetSettingLabel(field))),
        ...createAppOutputColumn(detail),
        ...taskEvaluators(detail).map((evaluator) => (
            tableColumn('evaluator', evaluator.taskEvaluatorId, evaluatorLabel(evaluator), evaluatorSettingLabel(evaluator))
        )),
        ...taskTags(detail).map((tag) => tableColumn('tag', tag.taskTagId, tag.tagName || '标签', tagSettingLabel(tag)))
    ];
}

function createAppOutputColumn(detail) {
    if (taskBase(detail)?.appType === 'agent') {
        return [tableColumn('appOutput', 'appOutput', '应用输出')];
    } else {
        return [];
    }
}

function tableColumn(type, refId, label, settingLabel = label) {
    return { id: `${type}:${refId}`, type, refId, label, settingLabel, visible: true };
}

function datasetSettingLabel(field) {
    return `评测集-${field.fieldName || '字段'}`;
}

function evaluatorLabel(evaluator) {
    return `${evaluator.evaluatorName || '-'} / ${evaluator.versionName || '-'}`;
}

function evaluatorSettingLabel(evaluator) {
    const name = evaluator.evaluatorName || evaluator.versionName || '-';
    return `${evaluatorSourceLabel(evaluator.evaluatorSource)}-${name}`;
}

function evaluatorSourceLabel(source) {
    if (source === 'custom') {
        return '自定义评估器';
    } else {
        return '预置评估器';
    }
}

function tagSettingLabel(tag) {
    return `标签-${tag.tagName || '-'}`;
}

function syncColumnSettings(ctx) {
    ctx.columnSettings.value = syncColumnSettingList(ctx.columnSettings.value, ctx.detail.value);
    ctx.columnSettingDraft.value = syncColumnSettingList(ctx.columnSettingDraft.value, ctx.detail.value);
}

function syncColumnSettingList(currentColumns, detail) {
    const nextColumns = createDefaultColumnSettings(detail);
    const currentById = new Map(currentColumns.map((item) => [item.id, item]));
    const nextById = new Map(nextColumns.map((item) => [item.id, item]));
    const ordered = currentColumns
        .map((item) => mergeColumnSetting(nextById.get(item.id), item))
        .filter(Boolean);
    const appended = nextColumns
        .filter((item) => !currentById.has(item.id))
        .map((item) => mergeColumnSetting(item));
    return [...ordered, ...appended];
}

function mergeColumnSetting(nextColumn, currentColumn) {
    if (!nextColumn) {
        return null;
    }
    return {
        ...nextColumn,
        visible: currentColumn?.visible ?? true
    };
}

function hydrateColumnSettings(settings, detail) {
    return settings
        .map((setting) => hydrateColumnSetting(setting, detail))
        .filter((column) => column.type === 'appOutput' || Boolean(column.target));
}

function hydrateColumnSetting(setting, detail) {
    const target = findColumnTarget(setting, detail);
    const label = target ? targetLabel(setting, target) : setting.label;
    const settingLabel = target ? targetSettingLabel(setting, target) : setting.settingLabel || label;
    return { ...setting, label, settingLabel, target };
}

function findColumnTarget(setting, detail) {
    if (setting.type === 'field') {
        return taskFields(detail).find((field) => field.id === setting.refId);
    } else if (setting.type === 'evaluator') {
        return taskEvaluators(detail).find((evaluator) => evaluator.taskEvaluatorId === setting.refId);
    } else if (setting.type === 'tag') {
        return taskTags(detail).find((tag) => tag.taskTagId === setting.refId);
    } else {
        return null;
    }
}

function targetLabel(setting, target) {
    if (setting.type === 'field') {
        return target.fieldName || setting.label;
    } else if (setting.type === 'evaluator') {
        return evaluatorLabel(target);
    } else if (setting.type === 'tag') {
        return target.tagName || setting.label;
    } else {
        return setting.label;
    }
}

function targetSettingLabel(setting, target) {
    if (setting.type === 'field') {
        return datasetSettingLabel(target);
    } else if (setting.type === 'evaluator') {
        return evaluatorSettingLabel(target);
    } else if (setting.type === 'tag') {
        return tagSettingLabel(target);
    } else {
        return setting.settingLabel || setting.label;
    }
}

function createColumnActions(ctx) {
    function setColumnVisible(columnId, visible) {
        ctx.columnSettingDraft.value = ctx.columnSettingDraft.value.map((item) => (
            item.id === columnId ? { ...item, visible } : item
        ));
    }
    function resetColumnSettings() {
        const defaults = createDefaultColumnSettings(ctx.detail.value);
        ctx.columnSettings.value = cloneColumnSettings(defaults);
        ctx.columnSettingDraft.value = cloneColumnSettings(defaults);
    }
    function confirmColumnSettings() {
        ctx.columnSettings.value = cloneColumnSettings(ctx.columnSettingDraft.value);
        ctx.columnSettingVisible.value = false;
    }
    function startColumnDrag(index) {
        ctx.draggedColumnIndex.value = index;
    }
    function enterColumnDrag(index) {
        if (ctx.draggedColumnIndex.value === null || ctx.draggedColumnIndex.value === index) {
            return;
        }
        moveColumn(ctx, ctx.draggedColumnIndex.value, index);
        ctx.draggedColumnIndex.value = index;
    }
    function finishColumnDrag() {
        ctx.draggedColumnIndex.value = null;
    }
    return { setColumnVisible, resetColumnSettings, confirmColumnSettings, startColumnDrag, enterColumnDrag, finishColumnDrag };
}

function moveColumn(ctx, fromIndex, toIndex) {
    const nextColumns = [...ctx.columnSettingDraft.value];
    const moved = nextColumns.splice(fromIndex, 1)[0];
    nextColumns.splice(toIndex, 0, moved);
    ctx.columnSettingDraft.value = nextColumns;
}

function cloneColumnSettings(settings) {
    return settings.map((item) => ({ ...item }));
}

export function useTaskDetail(taskId) {
    const router = useRouter();
    const state = createTaskDetailState();
    const ctx = createContext(taskId, state);
    const actions = createTaskDetailActions(ctx, router);
    const tagActions = createTagActions(ctx);
    const columnActions = createColumnActions(ctx);
    ctx.loadDetail = actions.loadDetail;
    const computedValues = createComputedValues(ctx.detail, ctx.columnSettings, ctx.columnSettingDraft);
    watchTaskDetail(ctx, computedValues, actions);
    return createTaskDetailReturn(ctx, computedValues, actions, tagActions, columnActions);
}

function createTaskDetailState() {
    return {
        loading: ref(false),
        stopping: ref(false),
        detail: ref(),
        page: ref(1),
        size: ref(10),
        tagDrawerVisible: ref(false),
        tagKeyword: ref(''),
        tagTypeFilter: ref(''),
        tagPage: ref(1),
        tagSize: ref(10),
        tagTotal: ref(0),
        tagLoading: ref(false),
        tagOperatingIds: ref([]),
        allTags: ref([]),
        columnSettingVisible: ref(false),
        columnSettings: ref([]),
        columnSettingDraft: ref([]),
        draggedColumnIndex: ref(null)
    };
}

function watchTaskDetail(ctx, computedValues, actions) {
    watch(ctx.taskId, async () => {
        await actions.loadDetail();
    }, { immediate: true });
    watch(() => computedValues.base.value?.status, (status) => {
        syncPollingByStatus(status, actions);
    });
    watch(ctx.detail, () => {
        syncColumnSettings(ctx);
    });
    watch(ctx.columnSettingVisible, (visible) => {
        if (visible) {
            ctx.columnSettingDraft.value = cloneColumnSettings(ctx.columnSettings.value);
        }
    });
    onBeforeUnmount(actions.stopPolling);
}

function createTaskDetailReturn(ctx, computedValues, actions, tagActions, columnActions) {
    return {
        ...refsToReturn(ctx),
        ...computedValues,
        ...actions,
        ...tagActions,
        ...columnActions,
        formatAppBinding: formatTaskAppBinding,
        statusLabel,
        passTagType,
        tagTypeLabel,
        formatTime: formatDateTime,
        tagTypeOptions: computed(() => TASK_TAG_TYPE_OPTIONS)
    };
}

function createContext(taskId, state) {
    return { taskId, ...state, pollTimer: undefined };
}

function createComputedValues(detail, columnSettings, columnSettingDraft) {
    const base = computed(() => taskBase(detail.value));
    return {
        base,
        evaluators: computed(() => taskEvaluators(detail.value)),
        tags: computed(() => taskTags(detail.value)),
        rows: computed(() => taskRows(detail.value)),
        total: computed(() => taskTotal(detail.value)),
        canStopTask: computed(() => STOPPABLE_STATUSES.includes(base.value?.status)),
        selectedTagIds: computed(() => taskTags(detail.value).map((tag) => tag.tagId)),
        columnSettingItems: computed(() => hydrateColumnSettings(columnSettingDraft.value, detail.value)),
        visibleTableColumns: computed(() => hydrateColumnSettings(columnSettings.value, detail.value).filter((column) => column.visible))
    };
}

function refsToReturn(ctx) {
    return {
        loading: ctx.loading,
        stopping: ctx.stopping,
        page: ctx.page,
        size: ctx.size,
        tagDrawerVisible: ctx.tagDrawerVisible,
        tagKeyword: ctx.tagKeyword,
        tagTypeFilter: ctx.tagTypeFilter,
        tagPage: ctx.tagPage,
        tagSize: ctx.tagSize,
        tagTotal: ctx.tagTotal,
        tagLoading: ctx.tagLoading,
        tagOperatingIds: ctx.tagOperatingIds,
        allTags: ctx.allTags,
        columnSettingVisible: ctx.columnSettingVisible
    };
}
