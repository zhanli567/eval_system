import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { evaluatorApi } from '../../../api/evaluator';
import { remoteCallApi } from '../../../api/remoteCall';
import { getErrorMessage } from '../../../utils/composableHelpers';
import { formatDateTime } from '../../../utils/formatters';

const DEFAULT_PROMPT = `你是一位专业的AI评估员。
请根据评分标准评估回复质量。

<查询>
\${query}
</查询>

<回复>
\${response}
</回复>

请只输出JSON，例如：{"score": 5, "reason": "准确且简洁。"}`;

const DEFAULT_CODE = `def evaluate(expected, actual):
    score = 5 if str(expected).strip() == str(actual).strip() else 1
    return {"score": score, "reason": "完全一致" if score == 5 else "内容不一致"}`;

function defaultParams() {
    return [
        { paramName: 'expected', dataType: 'string', defaultValue: '', required: true, description: '预期输出' },
        { paramName: 'actual', dataType: 'string', defaultValue: '', required: true, description: '实际输出' }
    ];
}

function createParam(paramName = '') {
    return { paramName, dataType: 'string', defaultValue: '', required: true, description: '' };
}

function cloneParam(param) {
    return {
        id: param.id,
        paramName: param.paramName,
        dataType: param.dataType,
        defaultValue: param.defaultValue || '',
        required: param.required ?? true,
        description: param.description || '',
        displayOrder: param.displayOrder
    };
}

function fillForm(form, config) {
    form.evaluatorName = config.evaluatorName;
    form.description = config.description;
    form.evaluatorType = config.evaluatorType;
    form.modelId = config.modelId || '';
    form.modelName = config.modelName || '';
    form.prompt = config.prompt || DEFAULT_PROMPT;
    form.executeCode = config.executeCode || DEFAULT_CODE;
    form.scoreMin = Number(config.scoreMin ?? 1);
    form.scoreMax = Number(config.scoreMax ?? 5);
    form.passThreshold = Number(config.passThreshold ?? 3);
    form.params = config.params.map(cloneParam);
}

function syncPromptParams(form) {
    const mappedParams = new Map(form.params.map((param) => [param.paramName, param]));
    form.params = extractPromptParams(form.prompt).map((name, index) => ({
        ...(mappedParams.has(name) ? cloneParam(mappedParams.get(name)) : createParam(name)),
        paramName: name,
        displayOrder: index + 1
    }));
    return form.params;
}

function ensureParamsByType(form) {
    if (form.evaluatorType === 'llm') {
        syncPromptParams(form);
    }
    if (!form.params.length && form.evaluatorType === 'code') {
        form.params = defaultParams();
    }
}

function extractPromptParams(prompt) {
    const result = [];
    const regex = /\$\{([a-zA-Z_][\w]*)\}/g;
    let match = regex.exec(prompt);
    while (match) {
        if (!result.includes(match[1])) {
            result.push(match[1]);
        }
        match = regex.exec(prompt);
    }
    return result;
}

function selectedModelName(form, models) {
    if (!form.modelId) {
        return '';
    }
    return models.value.find((model) => model.modelId === form.modelId)?.modelName || form.modelName || '';
}

function toParamPayload(param) {
    return {
        id: param.id,
        paramName: param.paramName.trim(),
        dataType: param.dataType || 'string',
        defaultValue: param.defaultValue || '',
        required: param.required ?? true,
        description: param.description?.trim() || '',
        displayOrder: param.displayOrder
    };
}

function payload(form, models) {
    const params = form.evaluatorType === 'llm' ? syncPromptParams(form) : form.params;
    return {
        evaluatorName: form.evaluatorName.trim(),
        evaluatorType: form.evaluatorType,
        description: form.description.trim(),
        modelId: form.evaluatorType === 'llm' ? form.modelId : '',
        modelName: form.evaluatorType === 'llm' ? selectedModelName(form, models) : '',
        prompt: form.evaluatorType === 'llm' ? form.prompt : '',
        executeCode: form.evaluatorType === 'code' ? form.executeCode : '',
        scoreMin: Number(form.scoreMin),
        scoreMax: Number(form.scoreMax),
        passThreshold: Number(form.passThreshold),
        params: params.map(toParamPayload)
    };
}

function validateForm(form, models) {
    if (!form.evaluatorName.trim()) {
        ElMessage.warning('请输入评估器名称');
        return false;
    }
    if ([form.scoreMin, form.scoreMax, form.passThreshold].some((value) => value === null || value === undefined || Number.isNaN(Number(value)))) {
        ElMessage.warning('请完善评分范围和通过阈值');
        return false;
    }
    if (form.scoreMin >= form.scoreMax) {
        ElMessage.warning('评分范围最大值必须大于最小值');
        return false;
    }
    if (form.passThreshold < form.scoreMin || form.passThreshold > form.scoreMax) {
        ElMessage.warning('通过阈值必须位于评分范围内');
        return false;
    }
    return validateEvaluatorBody(form, models);
}

function validateEvaluatorBody(form, models) {
    if (form.evaluatorType === 'code') {
        ElMessage.warning('暂不支持Code型评估器');
        return false;
    }
    if (!form.prompt.trim()) {
        ElMessage.warning('请输入Prompt');
        return false;
    }
    if (!form.modelId || !selectedModelName(form, models)) {
        ElMessage.warning('请选择模型');
        return false;
    }
    if (!extractPromptParams(form.prompt).length) {
        ElMessage.warning('Prompt至少需要包含一个${参数名}参数');
        return false;
    }
    return true;
}

function pickVersion(list, preferredVersionId) {
    return list.find((item) => item.id === preferredVersionId)
        ?? list.find((item) => item.draft)
        ?? list[list.length - 1];
}

function createEvaluatorEditorActions(ctx, router) {
    const modelActions = createModelActions(ctx);
    const versionActions = createVersionActions(ctx);
    const saveActions = createSaveActions(ctx, router, versionActions);
    const formActions = createFormActions(ctx, router);
    return { ...modelActions, ...versionActions, ...saveActions, ...formActions };
}

function createModelActions(ctx) {
    async function loadModelOptions() {
        if (ctx.models.value.length || ctx.modelLoading.value)
            return;
        ctx.modelLoading.value = true;
        try {
            ctx.models.value = await remoteCallApi.listModels();
        }
        catch (error) {
            ElMessage.error(getErrorMessage(error, '获取模型列表失败'));
        }
        finally {
            ctx.modelLoading.value = false;
        }
    }
    function clearModelOptions() {
        ctx.models.value = [];
    }
    async function handleModelVisibleChange(visible) {
        if (visible) {
            await loadModelOptions();
        }
    }
    return { loadModelOptions, clearModelOptions, handleModelVisibleChange };
}

function createVersionActions(ctx) {
    async function refreshEditor() {
        ctx.models.value = [];
        if (ctx.isEdit.value) {
            await loadVersions(ctx.activeVersionId.value);
        }
    }
    async function loadPreset(id) {
        ctx.loading.value = true;
        try {
            fillForm(ctx.form, await evaluatorApi.getPresetEvaluator(id));
            ensureParamsByType(ctx.form);
        }
        finally {
            ctx.loading.value = false;
        }
    }
    async function loadVersions(preferredVersionId) {
        if (!ctx.evaluatorId.value)
            return;
        ctx.loading.value = true;
        try {
            ctx.versions.value = await evaluatorApi.listVersions(ctx.evaluatorId.value);
            const fallback = pickVersion(ctx.versions.value, preferredVersionId);
            if (fallback) {
                await selectVersion(fallback.id);
            }
        }
        finally {
            ctx.loading.value = false;
        }
    }
    async function selectVersion(versionId) {
        ctx.activeVersionId.value = versionId;
        const detail = await evaluatorApi.getVersion(versionId);
        ctx.activeDetail.value = detail;
        fillForm(ctx.form, detail);
        ensureParamsByType(ctx.form);
    }
    return { refreshEditor, loadPreset, loadVersions, selectVersion };
}

function createSaveActions(ctx, router, versionActions) {
    async function submit() {
        if (!validateForm(ctx.form, ctx.models))
            return;
        ctx.saving.value = true;
        try {
            ctx.isEdit.value ? await submitDraft() : await createEvaluator();
        }
        catch (error) {
            ElMessage.error(getErrorMessage(error, ctx.isEdit.value ? '保存草稿失败' : '创建评估器失败'));
        }
        finally {
            ctx.saving.value = false;
        }
    }
    async function submitDraft() {
        if (!ctx.canEdit.value || !ctx.activeVersionId.value)
            return;
        const saved = await evaluatorApi.updateDraft(ctx.activeVersionId.value, payload(ctx.form, ctx.models));
        ctx.activeDetail.value = saved;
        ElMessage.success('草稿已保存');
        await versionActions.loadVersions(saved.versionId);
    }
    async function createEvaluator() {
        const name = ctx.form.evaluatorName.trim();
        const page = await evaluatorApi.listEvaluators({ page: 1, size: 100, keyword: name });
        if (page.records.some((evaluator) => evaluator.evaluatorName === name)) {
            throw new Error('当前空间已存在同名评估器');
        } else {
            const created = await evaluatorApi.createEvaluator(payload(ctx.form, ctx.models));
            ElMessage.success('评估器已创建');
            await router.replace({ name: 'evaluator-edit', params: { evaluatorId: created.evaluatorId } });
            await versionActions.loadVersions(created.versionId);
        }
    }
    async function publishDraft() {
        if (!ctx.isEdit.value || !ctx.evaluatorId.value)
            return;
        if (!ctx.canEdit.value) {
            ElMessage.warning('当前版本不可发布');
            return;
        }
        await ElMessageBox.confirm('发布后将生成新的只读版本，确定发布当前草稿吗？', '发布版本', { type: 'success' });
        ctx.publishing.value = true;
        try {
            if (!validateForm(ctx.form, ctx.models))
                return;
            await evaluatorApi.updateDraft(ctx.activeVersionId.value, payload(ctx.form, ctx.models));
            const published = await evaluatorApi.publish(ctx.evaluatorId.value);
            ElMessage.success(`已发布${published.versionName}`);
            await versionActions.loadVersions(published.versionId);
        }
        catch (error) {
            ElMessage.error(getErrorMessage(error, '发布版本失败'));
        }
        finally {
            ctx.publishing.value = false;
        }
    }
    async function removeVersion(version) {
        await ElMessageBox.confirm(`确定删除 ${version.versionName} 吗？`, '删除版本', { type: 'warning' });
        try {
            await evaluatorApi.deleteVersion(version.id);
            ElMessage.success('版本已删除');
            await versionActions.loadVersions();
        } catch (error) {
            ElMessage.error(getErrorMessage(error, '删除评估器版本失败'));
        }
    }
    return { submit, publishDraft, removeVersion };
}

function createFormActions(ctx, router) {
    function switchType(type) {
        if (type === 'code' || !ctx.canEdit.value || (ctx.isEdit.value && ctx.activeDetail.value?.evaluatorType !== type))
            return;
        ctx.form.evaluatorType = type;
        syncPromptParams(ctx.form);
    }
    function addParam() {
        ctx.form.params.push(createParam());
    }
    function removeParam(index) {
        ctx.form.params.splice(index, 1);
    }
    function changeParamType(index, dataType) {
        ctx.form.params[index].dataType = dataType;
    }
    function backToList() {
        router.push({ name: 'evaluators' });
    }
    return { switchType, addParam, removeParam, changeParamType, backToList };
}

function canEditValue(isEdit, activeDetail, evaluatorType) {
    return (!isEdit || Boolean(activeDetail?.draft)) && evaluatorType !== 'code';
}

function pageTitleValue(isEdit, evaluatorName) {
    return isEdit ? evaluatorName || '编辑评估器' : '创建评估器';
}

function promptParamsValue(form) {
    return form.evaluatorType === 'llm' ? form.params : [];
}

function modelOptionsValue(form, models) {
    const options = models.value.map((model) => ({
        label: model.name || model.modelName || model.modelId,
        value: model.modelId
    }));
    if (form.modelId && !options.some((option) => option.value === form.modelId)) {
        options.unshift({ label: form.modelName || form.modelId, value: form.modelId });
    }
    return options;
}

async function initEditor(actions, isEdit, presetId, form) {
    if (isEdit.value) {
        await actions.loadVersions();
    }
    else if (presetId.value) {
        await actions.loadPreset(presetId.value);
    }
    else {
        syncPromptParams(form);
    }
}

function syncPromptWhenLlm(form) {
    if (form.evaluatorType === 'llm') {
        syncPromptParams(form);
    }
}

export function useEvaluatorEditor() {
    const route = useRoute();
    const router = useRouter();
    const loading = ref(false);
    const saving = ref(false);
    const publishing = ref(false);
    const versions = ref([]);
    const activeVersionId = ref('');
    const activeDetail = ref(null);
    const models = ref([]);
    const modelLoading = ref(false);
    const form = reactive({
        evaluatorName: '',
        description: '',
        evaluatorType: 'llm',
        modelId: '',
        modelName: '',
        prompt: DEFAULT_PROMPT,
        executeCode: DEFAULT_CODE,
        scoreMin: 1,
        scoreMax: 5,
        passThreshold: 3,
        params: defaultParams()
    });
    const evaluatorId = computed(() => String(route.params.evaluatorId ?? ''));
    const presetId = computed(() => String(route.query.presetId ?? ''));
    const isEdit = computed(() => Boolean(evaluatorId.value));
    const canEdit = computed(() => canEditValue(isEdit.value, activeDetail.value, form.evaluatorType));
    const pageTitle = computed(() => pageTitleValue(isEdit.value, form.evaluatorName));
    const activeVersion = computed(() => versions.value.find((item) => item.id === activeVersionId.value));
    const promptParams = computed(() => promptParamsValue(form));
    const modelOptions = computed(() => modelOptionsValue(form, models));
    const ctx = { loading, saving, publishing, versions, activeVersionId, activeDetail, models, modelLoading, form, evaluatorId, presetId, isEdit, canEdit };
    const actions = createEvaluatorEditorActions(ctx, router);

    onMounted(async () => {
        await initEditor(actions, isEdit, presetId, form);
    });
    watch(() => [form.evaluatorType, form.prompt], () => {
        syncPromptWhenLlm(form);
    });

    return {
        loading,
        saving,
        publishing,
        versions,
        activeVersionId,
        activeDetail,
        form,
        isEdit,
        canEdit,
        pageTitle,
        activeVersion,
        promptParams,
        modelOptions,
        modelLoading,
        ...actions,
        formatTime: formatDateTime
    };
}
