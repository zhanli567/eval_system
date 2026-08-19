import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { evaluatorApi } from '../../../api/evaluator';
import { remoteCallApi } from '../../../api/remoteCall';
import { getErrorMessage } from '../../../utils/composableHelpers';
import { formatDateTime } from '../../../utils/formatters';
import { NUMBER_VALUE_RANGE_TEXT, hasNumberValueOutOfRange, isNumberValueMissing } from '../../../utils/numberRange';
import { formatPromptBlock } from '../../../utils/textBlocks';
import {
    EVALUATOR_TYPE_CODE,
    EVALUATOR_TYPE_EXACT_MATCH,
    EVALUATOR_TYPE_LLM,
    defaultParamsForEvaluatorType,
    requiresJudgeModel
} from '../../../utils/evaluatorTypes';

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
    return defaultParamsForEvaluatorType(EVALUATOR_TYPE_LLM);
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
    form.evaluatorType = config.evaluatorType || EVALUATOR_TYPE_LLM;
    form.modelId = config.modelId || '';
    form.modelName = config.modelName || '';
    form.prompt = formatPromptBlock(config.prompt || (form.evaluatorType === EVALUATOR_TYPE_LLM ? DEFAULT_PROMPT : ''));
    form.executeCode = config.executeCode || DEFAULT_CODE;
    form.scoreMin = Number(config.scoreMin ?? 1);
    form.scoreMax = Number(config.scoreMax ?? 5);
    form.passThreshold = Number(config.passThreshold ?? 3);
    form.params = (config.params || []).map(cloneParam);
    if (!form.params.length) {
        form.params = defaultParamsForEvaluatorType(form.evaluatorType);
    }
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
    if (form.evaluatorType === EVALUATOR_TYPE_LLM) {
        syncPromptParams(form);
    }
    if (!form.params.length && form.evaluatorType === EVALUATOR_TYPE_EXACT_MATCH) {
        form.params = defaultParamsForEvaluatorType(EVALUATOR_TYPE_EXACT_MATCH);
    }
    if (!form.params.length && form.evaluatorType === EVALUATOR_TYPE_CODE) {
        form.params = defaultParamsForEvaluatorType(EVALUATOR_TYPE_CODE);
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
    const params = form.evaluatorType === EVALUATOR_TYPE_LLM ? syncPromptParams(form) : form.params;
    return {
        evaluatorName: form.evaluatorName.trim(),
        evaluatorType: form.evaluatorType,
        description: form.description.trim(),
        modelId: requiresJudgeModel(form.evaluatorType) ? form.modelId : '',
        modelName: requiresJudgeModel(form.evaluatorType) ? selectedModelName(form, models) : '',
        prompt: form.evaluatorType === EVALUATOR_TYPE_LLM ? form.prompt : '',
        executeCode: form.evaluatorType === EVALUATOR_TYPE_CODE ? form.executeCode : '',
        scoreMin: Number(form.scoreMin),
        scoreMax: Number(form.scoreMax),
        passThreshold: Number(form.passThreshold),
        params: params.map(toParamPayload)
    };
}

function draftPayload(form, models) {
    const data = payload(form, models);
    delete data.evaluatorName;
    delete data.description;
    return data;
}

function validateForm(form, models) {
    if (!form.evaluatorName.trim()) {
        ElMessage.warning('请输入评估器名称');
        return false;
    }
    if (!validateScoreConfig(form)) {
        return false;
    }
    return validateEvaluatorBody(form, models);
}

function validateScoreConfig(form) {
    const values = [form.scoreMin, form.scoreMax, form.passThreshold];
    if (values.some((value) => isNumberValueMissing(value))) {
        ElMessage.warning('请完善评分范围和通过阈值');
        return false;
    }
    if (hasNumberValueOutOfRange(values)) {
        ElMessage.warning(`评分范围和通过阈值必须在${NUMBER_VALUE_RANGE_TEXT}之间`);
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
    return true;
}

function validateEvaluatorBody(form, models) {
    if (form.evaluatorType === EVALUATOR_TYPE_CODE) {
        ElMessage.warning('暂不支持Code型评估器');
        return false;
    }
    if (form.evaluatorType === EVALUATOR_TYPE_EXACT_MATCH) {
        if (form.params.length < 2 || form.params[0]?.paramName !== 'expected' || form.params[1]?.paramName !== 'actual') {
            ElMessage.warning('精确匹配需要 expected 和 actual 两个参数');
            return false;
        }
        return true;
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

function validateTrialForm(form, models) {
    if (!validateScoreConfig(form)) {
        return false;
    }
    return validateEvaluatorBody(form, models);
}

function pickVersion(list, preferredVersionId) {
    return list.find((item) => item.id === preferredVersionId)
        ?? list.find((item) => item.draft)
        ?? list[list.length - 1];
}

function createEvaluatorEditorActions(ctx, router) {
    const modelActions = createModelActions(ctx);
    const versionActions = createVersionActions(ctx);
    const presetPickerActions = createPresetPickerActions(ctx);
    const trialActions = createTrialActions(ctx);
    const saveActions = createSaveActions(ctx, router, versionActions);
    const formActions = createFormActions(ctx, router);
    return { ...modelActions, ...versionActions, ...presetPickerActions, ...trialActions, ...saveActions, ...formActions };
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

function createPresetPickerActions(ctx) {
    return {
        openPresetPicker: () => openPresetPicker(ctx),
        searchPreset: () => searchPreset(ctx),
        selectPresetCategory: (categoryId) => selectPresetCategory(ctx, categoryId),
        changePresetPage: () => loadPresetEvaluators(ctx),
        usePresetEvaluator: (presetId) => usePresetEvaluator(ctx, presetId)
    };
}

async function openPresetPicker(ctx) {
    ctx.presetPickerVisible.value = true;
    if (!ctx.presetCategories.value.length) {
        await loadPresetCategories(ctx);
    } else {
        await loadPresetEvaluators(ctx);
    }
}

async function loadPresetCategories(ctx) {
    const categories = await evaluatorApi.listPresetCategories();
    ctx.presetCategories.value = [
        { id: '', categoryName: '全部分类', displayOrder: 0 },
        ...categories
    ];
    await loadPresetEvaluators(ctx);
}

async function loadPresetEvaluators(ctx) {
    ctx.presetLoading.value = true;
    try {
        const page = await evaluatorApi.listPresetEvaluators({
            page: ctx.presetPage.value,
            size: ctx.presetSize.value,
            categoryId: ctx.presetCategoryId.value,
            keyword: ctx.presetKeyword.value
        });
        ctx.presetEvaluators.value = page.records;
        ctx.presetTotal.value = page.total;
    } finally {
        ctx.presetLoading.value = false;
    }
}

async function searchPreset(ctx) {
    ctx.presetPage.value = 1;
    await loadPresetEvaluators(ctx);
}

async function selectPresetCategory(ctx, categoryId) {
    ctx.presetCategoryId.value = categoryId;
    ctx.presetPage.value = 1;
    await loadPresetEvaluators(ctx);
}

async function usePresetEvaluator(ctx, presetId) {
    const detail = await evaluatorApi.getPresetEvaluator(presetId);
    if (detail.evaluatorType === 'code') {
        ElMessage.warning('暂不支持Code型评估器');
    } else {
        fillForm(ctx.form, detail);
        ensureParamsByType(ctx.form);
        syncTrialParamValues(ctx.form, ctx.trialParamValues);
        ctx.presetPickerVisible.value = false;
    }
}

function createTrialActions(ctx) {
    async function runTrial() {
        if (!validateTrialForm(ctx.form, ctx.models)) {
            return;
        }
        ctx.trialLoading.value = true;
        ctx.trialResult.value = null;
        try {
            ctx.trialResult.value = await evaluatorApi.runTrial({
                evaluator: payload(ctx.form, ctx.models),
                paramValues: { ...ctx.trialParamValues }
            });
        } finally {
            ctx.trialLoading.value = false;
        }
    }
    function clearTrialResult() {
        ctx.trialResult.value = null;
    }
    return { runTrial, clearTrialResult };
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
        const saved = await evaluatorApi.updateDraft(ctx.activeVersionId.value, draftPayload(ctx.form, ctx.models));
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
            await evaluatorApi.updateDraft(ctx.activeVersionId.value, draftPayload(ctx.form, ctx.models));
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
        await evaluatorApi.deleteVersion(version.id);
        ElMessage.success('版本已删除');
        await versionActions.loadVersions();
    }
    return { submit, publishDraft, removeVersion };
}

function createFormActions(ctx, router) {
    function switchType(type) {
        if (![EVALUATOR_TYPE_LLM, EVALUATOR_TYPE_EXACT_MATCH].includes(type)
            || !ctx.canEdit.value
            || (ctx.isEdit.value && ctx.activeDetail.value?.evaluatorType !== type))
            return;
        ctx.form.evaluatorType = type;
        if (type === EVALUATOR_TYPE_LLM) {
            ctx.form.prompt = DEFAULT_PROMPT;
            syncPromptParams(ctx.form);
        } else {
            ctx.form.prompt = '';
            ctx.form.params = defaultParamsForEvaluatorType(type);
            ctx.form.modelId = '';
            ctx.form.modelName = '';
        }
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
    async function copyPrompt() {
        if (navigator.clipboard?.writeText) {
            await navigator.clipboard.writeText(ctx.form.prompt || '');
            ElMessage.success('Prompt已复制');
        } else {
            ElMessage.warning('当前浏览器不支持复制到剪贴板');
        }
    }
    function clearPrompt() {
        ctx.form.prompt = '';
    }
    function backToList() {
        router.push({ name: 'evaluators' });
    }
    return { switchType, addParam, removeParam, changeParamType, copyPrompt, clearPrompt, backToList };
}

function canEditValue(isEdit, activeDetail, evaluatorType) {
    return (!isEdit || Boolean(activeDetail?.draft)) && evaluatorType !== EVALUATOR_TYPE_CODE;
}

function pageTitleValue(isEdit, evaluatorName) {
    return isEdit ? evaluatorName || '' : '创建评估器';
}

function promptParamsValue(form) {
    return form.evaluatorType === EVALUATOR_TYPE_LLM ? form.params : [];
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
    if (form.evaluatorType === EVALUATOR_TYPE_LLM) {
        syncPromptParams(form);
    }
}

function syncTrialParamValues(form, values) {
    const names = new Set(form.params.map((param) => param.paramName));
    Object.keys(values).forEach((name) => {
        if (!names.has(name)) {
            delete values[name];
        }
    });
    form.params.forEach((param) => {
        if (values[param.paramName] === undefined) {
            values[param.paramName] = param.defaultValue || '';
        }
    });
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
    const presetPickerVisible = ref(false);
    const presetCategories = ref([]);
    const presetEvaluators = ref([]);
    const presetPage = ref(1);
    const presetSize = ref(9);
    const presetTotal = ref(0);
    const presetKeyword = ref('');
    const presetCategoryId = ref('');
    const presetLoading = ref(false);
    const trialLoading = ref(false);
    const trialResult = ref(null);
    const trialParamValues = reactive({});
    const form = reactive({
        evaluatorName: '',
        description: '',
        evaluatorType: EVALUATOR_TYPE_LLM,
        modelId: '',
        modelName: '',
        prompt: DEFAULT_PROMPT,
        executeCode: DEFAULT_CODE,
        scoreMin: 1,
        scoreMax: 5,
        passThreshold: 3,
        params: defaultParamsForEvaluatorType(EVALUATOR_TYPE_LLM)
    });
    const evaluatorId = computed(() => String(route.params.evaluatorId ?? ''));
    const presetId = computed(() => String(route.query.presetId ?? ''));
    const isEdit = computed(() => Boolean(evaluatorId.value));
    const canEdit = computed(() => canEditValue(isEdit.value, activeDetail.value, form.evaluatorType));
    const pageTitle = computed(() => pageTitleValue(isEdit.value, form.evaluatorName));
    const activeVersion = computed(() => versions.value.find((item) => item.id === activeVersionId.value));
    const promptParams = computed(() => promptParamsValue(form));
    const modelOptions = computed(() => modelOptionsValue(form, models));
    const ctx = { loading, saving, publishing, versions, activeVersionId, activeDetail, models, modelLoading, presetPickerVisible, presetCategories, presetEvaluators, presetPage, presetSize, presetTotal, presetKeyword, presetCategoryId, presetLoading, trialLoading, trialResult, trialParamValues, form, evaluatorId, presetId, isEdit, canEdit };
    const actions = createEvaluatorEditorActions(ctx, router);

    onMounted(async () => {
        await initEditor(actions, isEdit, presetId, form);
        syncTrialParamValues(form, trialParamValues);
    });
    watch(() => [form.evaluatorType, form.prompt], () => {
        syncPromptWhenLlm(form);
        syncTrialParamValues(form, trialParamValues);
    });
    watch(() => form.params.map((param) => `${param.paramName}:${param.defaultValue || ''}`), () => {
        syncTrialParamValues(form, trialParamValues);
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
        presetPickerVisible,
        presetCategories,
        presetEvaluators,
        presetPage,
        presetSize,
        presetTotal,
        presetKeyword,
        presetCategoryId,
        presetLoading,
        trialLoading,
        trialResult,
        trialParamValues,
        ...actions,
        formatTime: formatDateTime
    };
}
