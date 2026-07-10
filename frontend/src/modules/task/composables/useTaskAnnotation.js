import { computed, reactive, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { taskApi } from '../../../api/task';
import { getErrorMessage } from '../../../utils/composableHelpers';
import { passTagType, tagTypeLabel } from '../../../utils/taskLabels';

function formValueByTag(tag) {
    if (tag.tagType === 'number') {
        return tag.result?.valueNumber;
    }
    if (tag.tagType === 'category' || tag.tagType === 'boolean') {
        return tag.result?.tagOptionId || '';
    }
    return tag.result?.valueText || '';
}

function annotationPayload(tag, form) {
    const value = form[tag.taskTagId];
    return {
        taskTagId: tag.taskTagId,
        valueText: tag.tagType === 'text' ? String(value ?? '') : undefined,
        valueNumber: tag.tagType === 'number' && value !== undefined && value !== '' ? Number(value) : undefined,
        tagOptionId: tag.tagType === 'category' || tag.tagType === 'boolean' ? String(value ?? '') : undefined
    };
}

function hasTagValue(tag, form) {
    const value = form[tag.taskTagId];
    return tag.tagType === 'number' ? value !== undefined && value !== '' : Boolean(String(value ?? '').trim());
}

function optionLabel(tag) {
    if (tag.tagType === 'number') {
        return `范围 ${tag.minValue ?? '-'}-${tag.maxValue ?? '-'}，通过阈值 ${tag.passThreshold ?? '-'}`;
    }
    return tag.description || '暂无描述';
}

function appOutputEmptyDescription(task, item) {
    if (!task) {
        return '标注数据加载后展示应用输出';
    }
    if (task.appType !== 'agent') {
        return '当前任务未关联应用，无应用输出';
    }
    if (item?.appOutputStatus === 'failed') {
        return '应用调用失败，暂无应用输出';
    }
    return item?.appOutputStatus === 'pending' ? '应用输出待生成' : '暂无应用输出';
}

function createAnnotationActions(ctx, router) {
    async function loadAnnotation() {
        if (!ctx.taskId.value || !ctx.taskItemId.value)
            return;
        ctx.loading.value = true;
        ctx.loadError.value = '';
        try {
            ctx.detail.value = await taskApi.getAnnotation(ctx.taskId.value, ctx.taskItemId.value);
            fillForm();
        }
        catch (error) {
            ctx.detail.value = undefined;
            ctx.loadError.value = getErrorMessage(error, '加载标注数据失败');
            ElMessage.error(ctx.loadError.value);
        }
        finally {
            ctx.loading.value = false;
        }
    }
    function fillForm() {
        Object.keys(ctx.form).forEach((key) => delete ctx.form[key]);
        ctx.tags.value.forEach((tag) => {
            ctx.form[tag.taskTagId] = formValueByTag(tag);
        });
    }
    async function saveAnnotation() {
        if (!ctx.taskId.value || !ctx.taskItemId.value || !validate())
            return;
        ctx.saving.value = true;
        try {
            ctx.detail.value = await taskApi.saveAnnotation(ctx.taskId.value, ctx.taskItemId.value, {
                tags: ctx.tags.value.map((tag) => annotationPayload(tag, ctx.form))
            });
            fillForm();
            ElMessage.success('标注已保存');
        }
        catch (error) {
            ElMessage.error(getErrorMessage(error, '保存标注失败'));
        }
        finally {
            ctx.saving.value = false;
        }
    }
    function validate() {
        const missingTag = ctx.tags.value.find((tag) => !hasTagValue(tag, ctx.form));
        if (missingTag) {
            ElMessage.warning(`请完成${missingTag.tagName}`);
            return false;
        }
        return true;
    }
    function backToDetail() {
        router.push({ name: 'task-detail', params: { taskId: ctx.taskId.value } });
    }
    function goItem(targetItemId) {
        if (targetItemId) {
            router.push({ name: 'task-annotation', params: { taskId: ctx.taskId.value, taskItemId: targetItemId } });
        }
    }
    return { loadAnnotation, saveAnnotation, backToDetail, goItem };
}

const annotationTask = (detail) => detail?.task;
const annotationItem = (detail) => detail?.item;
const annotationFields = (detail) => detail?.fields ?? [];
const annotationTags = (detail) => detail?.tags ?? [];
const annotationEvaluators = (detail) => detail?.evaluators ?? [];
const previousItemId = (detail) => detail?.previousItemId || '';
const nextItemId = (detail) => detail?.nextItemId || '';

export function useTaskAnnotation(taskId, taskItemId) {
    const router = useRouter();
    const loading = ref(false);
    const saving = ref(false);
    const loadError = ref('');
    const detail = ref();
    const form = reactive({});
    const task = computed(() => annotationTask(detail.value));
    const item = computed(() => annotationItem(detail.value));
    const fields = computed(() => annotationFields(detail.value));
    const tags = computed(() => annotationTags(detail.value));
    const evaluators = computed(() => annotationEvaluators(detail.value));
    const previousItem = computed(() => previousItemId(detail.value));
    const nextItem = computed(() => nextItemId(detail.value));
    const ctx = { taskId, taskItemId, loading, saving, loadError, detail, form, tags };
    const actions = createAnnotationActions(ctx, router);
    watch(() => [taskId.value, taskItemId.value], async () => {
        await actions.loadAnnotation();
    }, { immediate: true });

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
        previousItemId: previousItem,
        nextItemId: nextItem,
        loadAnnotation: actions.loadAnnotation,
        saveAnnotation: actions.saveAnnotation,
        backToDetail: actions.backToDetail,
        goItem: actions.goItem,
        passTagType,
        tagTypeLabel,
        optionLabel,
        appOutputEmptyDescription: () => appOutputEmptyDescription(task.value, item.value)
    };
}
