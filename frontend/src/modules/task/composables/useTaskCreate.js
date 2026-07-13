import { computed, reactive, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { datasetApi } from '../../../api/dataset';
import { evaluatorApi } from '../../../api/evaluator';
import { remoteCallApi } from '../../../api/remoteCall';
import { tagApi } from '../../../api/tag';
import { taskApi } from '../../../api/task';
import { getErrorMessage } from '../../../utils/composableHelpers';
import { tagTypeLabel } from '../../../utils/taskLabels';

const TASK_TAG_TYPE_OPTIONS = [
    { value: 'category', label: '分类' },
    { value: 'boolean', label: '布尔' },
    { value: 'number', label: '数字' },
    { value: 'text', label: '文本' }
];

function createEvaluatorBlock() {
    return {
        key: `${Date.now()}-${Math.random()}`,
        evaluatorSource: 'preset',
        presetCategoryId: '',
        evaluatorId: '',
        evaluatorVersionId: '',
        modelId: '',
        evaluatorName: '',
        evaluatorType: '',
        description: '',
        versionName: '',
        prompt: '',
        executeCode: '',
        params: [],
        paramMappings: {},
        presetOptions: [],
        presetOptionsLoaded: false,
        versions: [],
        detailExpanded: false,
        loading: false
    };
}

function createState() {
    return {
        loading: ref(false),
        saving: ref(false),
        tagDrawerVisible: ref(false),
        tagKeyword: ref(''),
        tagTypeFilter: ref(''),
        datasets: ref([]),
        versions: ref([]),
        fields: ref([]),
        tags: ref([]),
        selectedTagIds: ref([]),
        customEvaluators: ref([]),
        presetCategories: ref([]),
        evaluatorBlocks: ref([]),
        agents: ref([]),
        agentDetails: reactive({}),
        agentBundleVersions: reactive({}),
        models: ref([]),
        appFieldMappings: reactive({}),
        datasetsLoaded: ref(false),
        tagsLoaded: ref(false),
        customEvaluatorsLoaded: ref(false),
        presetCategoriesLoaded: ref(false),
        agentsLoaded: ref(false),
        agentDetailLoading: ref(false),
        agentVersionLoading: ref(false),
        modelsLoaded: ref(false),
        modelLoading: ref(false),
        form: reactive({
            taskName: '',
            description: '',
            datasetId: '',
            datasetVersionId: '',
            appType: 'none',
            appId: '',
            appVersionId: '',
            appAgentAlias: ''
        })
    };
}

function createComputedValues(state) {
    const selectedVersion = computed(() => state.versions.value.find((item) => item.id === state.form.datasetVersionId));
    const selectedAgent = computed(() => state.agentDetails[state.form.appId] || state.agents.value.find((agent) => agent.id === state.form.appId));
    const agentVersions = computed(() => state.form.appId ? state.agentBundleVersions[state.form.appId] || [] : []);
    const agentOutputs = computed(() => selectedAgent.value?.outputs || defaultAgentOutputs());
    return {
        selectedVersion,
        publishedVersions: computed(() => state.versions.value.filter((item) => !item.draft).sort((a, b) => b.versionNo - a.versionNo)),
        canSubmit: computed(() => Boolean(state.form.taskName.trim() && state.form.datasetVersionId && (state.evaluatorBlocks.value.length || state.selectedTagIds.value.length))),
        categoryOptions: computed(() => [{ id: '', categoryName: '全部分类', displayOrder: 0 }, ...state.presetCategories.value]),
        selectedAgent,
        agentVersions,
        agentChildAgents: computed(() => selectedAgent.value?.childAgents || []),
        agentInputs: computed(() => selectedAgent.value?.inputs || []),
        agentOutputs,
        tagTypeOptions: computed(() => TASK_TAG_TYPE_OPTIONS),
        selectedTags: computed(() => state.selectedTagIds.value.map((tagId) => state.tags.value.find((tag) => tag.id === tagId)).filter(Boolean)),
        filteredTags: computed(() => filterTags(state.tags.value, state.tagKeyword.value, state.tagTypeFilter.value))
    };
}

function filterTags(tags, keywordValue, tagType) {
    const keyword = keywordValue.trim().toLowerCase();
    return tags.filter((tag) => {
        const matchesType = !tagType || tag.tagType === tagType;
        const matchesKeyword = !keyword || tag.tagName.toLowerCase().includes(keyword) || tag.description.toLowerCase().includes(keyword);
        return matchesType && matchesKeyword;
    });
}

function createLoadActions(ctx) {
    const loaders = createRawLoaders(ctx);
    return { ...loaders, ...createEnsureLoaders(ctx, loaders) };
}

function createEnsureLoaders(ctx, loaders) {
    return {
        ...createBasicEnsureLoaders(ctx, loaders),
        ...createRemoteEnsureLoaders(ctx),
        ...createEvaluatorEnsureLoaders(loaders)
    };
}

async function withTaskCreatePageLoading(ctx, action, fallback) {
    ctx.state.loading.value = true;
    try {
        await action();
    }
    catch (error) {
        ElMessage.error(getErrorMessage(error, fallback));
    }
    finally {
        ctx.state.loading.value = false;
    }
}

function createBasicEnsureLoaders(ctx, loaders) {
    async function ensureDatasetsLoaded() {
        if (ctx.state.datasetsLoaded.value)
            return;
        await withTaskCreatePageLoading(ctx, async () => {
            await loaders.loadDatasets();
            ctx.state.datasetsLoaded.value = true;
        }, '获取评测集失败');
    }
    async function ensureTagsLoaded() {
        if (ctx.state.tagsLoaded.value)
            return;
        await withTaskCreatePageLoading(ctx, async () => {
            await loaders.loadTags();
            ctx.state.tagsLoaded.value = true;
        }, '获取标签失败');
    }
    async function ensureCustomEvaluatorsLoaded() {
        if (ctx.state.customEvaluatorsLoaded.value)
            return;
        await withTaskCreatePageLoading(ctx, async () => {
            await loaders.loadCustomEvaluators();
            ctx.state.customEvaluatorsLoaded.value = true;
        }, '获取自定义评估器失败');
    }
    async function ensurePresetCategoriesLoaded() {
        if (ctx.state.presetCategoriesLoaded.value)
            return;
        await withTaskCreatePageLoading(ctx, async () => {
            await loaders.loadPresetCategories();
            ctx.state.presetCategoriesLoaded.value = true;
        }, '获取预置评估器分类失败');
    }
    async function ensureAgentsLoaded() {
        if (ctx.state.agentsLoaded.value)
            return;
        await withTaskCreatePageLoading(ctx, async () => {
            await loaders.loadAgents();
            ctx.state.agentsLoaded.value = true;
        }, '获取智能体列表失败');
    }
    return { ensureDatasetsLoaded, ensureTagsLoaded, ensureCustomEvaluatorsLoaded, ensurePresetCategoriesLoaded, ensureAgentsLoaded };
}

function createRemoteEnsureLoaders(ctx) {
    async function ensureModelsLoaded() {
        if (ctx.state.modelsLoaded.value || ctx.state.modelLoading.value)
            return;
        ctx.state.modelLoading.value = true;
        try {
            ctx.state.models.value = await remoteCallApi.listModels();
            ctx.state.modelsLoaded.value = true;
        }
        catch (error) {
            ElMessage.error(getErrorMessage(error, '获取模型列表失败'));
        }
        finally {
            ctx.state.modelLoading.value = false;
        }
    }
    async function ensureAgentDetailLoaded(agentId) {
        if (!agentId || ctx.state.agentDetails[agentId])
            return;
        await loadAgentCache(agentId, ctx.state.agentDetailLoading, remoteCallApi.getAgentDetail, ctx.state.agentDetails, '获取智能体详情失败');
    }
    async function ensureAgentBundlesLoaded(agentId) {
        if (!agentId || ctx.state.agentBundleVersions[agentId])
            return;
        await loadAgentCache(agentId, ctx.state.agentVersionLoading, remoteCallApi.listAgentBundles, ctx.state.agentBundleVersions, '获取智能体快照失败');
    }
    return { ensureAgentDetailLoaded, ensureAgentBundlesLoaded, ensureModelsLoaded };
}

function createEvaluatorEnsureLoaders(loaders) {
    async function ensurePresetOptionsLoaded(block, force = false) {
        if (!force && block.presetOptionsLoaded)
            return;
        block.loading = true;
        try {
            await loaders.loadPresetOptions(block);
            block.presetOptionsLoaded = true;
        }
        catch (error) {
            ElMessage.error(getErrorMessage(error, '获取预置评估器失败'));
        }
        finally {
            block.loading = false;
        }
    }
    return { ensurePresetOptionsLoaded };
}

function createRawLoaders(ctx) {
    async function loadDatasets() {
        const page = await datasetApi.listDatasets({ page: 1, size: 100 });
        ctx.state.datasets.value = page.records;
    }
    async function loadVersions() {
        ctx.state.versions.value = [];
        ctx.state.fields.value = [];
        ctx.state.form.datasetVersionId = '';
        if (!ctx.state.form.datasetId)
            return;
        ctx.state.versions.value = await datasetApi.listVersions(ctx.state.form.datasetId);
        ctx.state.form.datasetVersionId = ctx.computed.publishedVersions.value[0]?.id || '';
    }
    async function loadFields() {
        ctx.state.fields.value = [];
        if (!ctx.state.form.datasetVersionId)
            return;
        const detail = await datasetApi.getVersionDetail(ctx.state.form.datasetVersionId, { page: 1, size: 1 });
        ctx.state.fields.value = detail.fields;
        ctx.state.evaluatorBlocks.value.forEach((block) => ensureParamMappings(ctx, block));
        ensureAppFieldMappings(ctx);
    }
    async function loadTags() {
        const page = await tagApi.listTags({ page: 1, size: 100 });
        ctx.state.tags.value = page.records;
    }
    async function loadCustomEvaluators() {
        const page = await evaluatorApi.listEvaluators({ page: 1, size: 100 });
        ctx.state.customEvaluators.value = page.records;
    }
    async function loadPresetCategories() {
        ctx.state.presetCategories.value = await evaluatorApi.listPresetCategories();
    }
    async function loadAgents() {
        ctx.state.agents.value = await remoteCallApi.listAgents();
        if (ctx.state.form.appType === 'agent') {
            selectDefaultAgent(ctx);
        }
    }
    async function loadPresetOptions(block) {
        const page = await evaluatorApi.listPresetEvaluators({
            page: 1,
            size: 100,
            categoryId: block.presetCategoryId,
            keyword: ''
        });
        block.presetOptions = page.records;
    }
    return {
        loadDatasets,
        loadTags,
        loadCustomEvaluators,
        loadPresetCategories,
        loadAgents,
        loadPresetOptions,
        loadVersions,
        loadFields
    };
}

async function loadAgentCache(agentId, loading, apiCall, cache, fallback) {
    loading.value = true;
    try {
        cache[agentId] = await apiCall(agentId);
    }
    catch (error) {
        ElMessage.error(getErrorMessage(error, fallback));
    }
    finally {
        loading.value = false;
    }
}

function createVisibleHandlers(loadActions) {
    return {
        handleDatasetVisible: (visible) => visible && loadActions.ensureDatasetsLoaded(),
        handleAgentVisible: (visible) => visible && loadActions.ensureAgentsLoaded(),
        handleCustomEvaluatorVisible: (visible) => visible && loadActions.ensureCustomEvaluatorsLoaded(),
        handlePresetCategoryVisible: (visible) => visible && loadActions.ensurePresetCategoriesLoaded(),
        handlePresetEvaluatorVisible: (block, visible) => visible && loadActions.ensurePresetOptionsLoaded(block),
        handleModelVisible: (visible) => visible && loadActions.ensureModelsLoaded()
    };
}

function createEvaluatorActions(ctx, loadActions) {
    async function changePresetCategory(block) {
        block.evaluatorId = '';
        clearEvaluatorDetail(block);
        block.presetOptions = [];
        block.presetOptionsLoaded = false;
        await loadActions.ensurePresetOptionsLoaded(block, true);
    }
    async function changeEvaluatorSource(block) {
        block.evaluatorId = '';
        block.evaluatorVersionId = '';
        block.modelId = '';
        clearEvaluatorDetail(block);
        if (block.evaluatorSource === 'preset') {
            block.presetOptions = [];
            block.presetOptionsLoaded = false;
        }
    }
    async function selectEvaluator(block) {
        if (!block.evaluatorId) {
            clearEvaluatorDetail(block);
            return;
        }
        block.loading = true;
        try {
            await fillSelectedEvaluator(ctx, block);
        }
        finally {
            block.loading = false;
        }
    }
    async function selectCustomVersion(block) {
        if (!block.evaluatorVersionId)
            return;
        block.loading = true;
        try {
            const detail = await evaluatorApi.getVersion(block.evaluatorVersionId);
            if (!rejectCodeEvaluator(block, detail)) {
                fillFromCustom(ctx, block, detail);
            }
        }
        finally {
            block.loading = false;
        }
    }
    function addEvaluator() {
        if (ctx.state.evaluatorBlocks.value.length >= 5) {
            ElMessage.warning('评估器最多添加5个');
            return;
        }
        ctx.state.evaluatorBlocks.value.push(createEvaluatorBlock());
    }
    function removeEvaluator(index) {
        ctx.state.evaluatorBlocks.value.splice(index, 1);
    }
    return { changePresetCategory, changeEvaluatorSource, selectEvaluator, selectCustomVersion, addEvaluator, removeEvaluator };
}

async function fillSelectedEvaluator(ctx, block) {
    if (block.evaluatorSource === 'preset') {
        const detail = await evaluatorApi.getPresetEvaluator(block.evaluatorId);
        if (!rejectCodeEvaluator(block, detail)) {
            fillFromPreset(ctx, block, detail);
        }
        return;
    }
    block.versions = (await evaluatorApi.listVersions(block.evaluatorId)).filter((version) => !version.draft);
    block.evaluatorVersionId = [...block.versions].sort((a, b) => b.versionNo - a.versionNo)[0]?.id || '';
    if (block.evaluatorVersionId) {
        const detail = await evaluatorApi.getVersion(block.evaluatorVersionId);
        if (!rejectCodeEvaluator(block, detail)) {
            fillFromCustom(ctx, block, detail);
        }
    }
}

function rejectCodeEvaluator(block, detail) {
    if (detail.evaluatorType !== 'code') {
        return false;
    }
    ElMessage.warning('暂不支持Code型评估器');
    block.evaluatorId = '';
    block.evaluatorVersionId = '';
    clearEvaluatorDetail(block);
    return true;
}

function fillFromPreset(ctx, block, detail) {
    fillEvaluatorBase(block, detail);
    block.versionName = '预置';
    block.modelId = detail.modelId || '';
    ensureParamMappings(ctx, block);
}

function fillFromCustom(ctx, block, detail) {
    fillEvaluatorBase(block, detail);
    block.versionName = detail.versionName;
    block.modelId = '';
    ensureParamMappings(ctx, block);
}

function fillEvaluatorBase(block, detail) {
    block.evaluatorName = detail.evaluatorName;
    block.evaluatorType = detail.evaluatorType;
    block.description = detail.description;
    block.scoreMin = Number(detail.scoreMin);
    block.scoreMax = Number(detail.scoreMax);
    block.passThreshold = Number(detail.passThreshold);
    block.prompt = detail.prompt;
    block.executeCode = detail.executeCode;
    block.params = detail.params;
}

function clearEvaluatorDetail(block) {
    Object.assign(block, {
        evaluatorName: '',
        evaluatorType: '',
        description: '',
        versionName: '',
        modelId: '',
        prompt: '',
        executeCode: '',
        scoreMin: undefined,
        scoreMax: undefined,
        passThreshold: undefined,
        params: [],
        paramMappings: {},
        versions: []
    });
    if (block.evaluatorSource !== 'preset') {
        block.presetOptions = [];
        block.presetOptionsLoaded = false;
    }
}

function ensureParamMappings(ctx, block) {
    block.paramMappings = Object.fromEntries(block.params.map((param) => {
        const key = paramKey(param);
        return [key, block.paramMappings[key] || defaultParamMapping(ctx)];
    }));
}

function defaultParamMapping(ctx) {
    return { sourceType: 'dataset_field', datasetFieldId: '', appOutputName: defaultAppOutputName(ctx) };
}

function createAppActions(ctx, loadActions) {
    async function selectAgent(agentId) {
        ctx.state.form.appAgentAlias = '';
        ctx.state.form.appVersionId = '';
        if (agentId) {
            await Promise.all([
                loadActions.ensureAgentDetailLoaded(agentId),
                loadActions.ensureAgentBundlesLoaded(agentId)
            ]);
        }
        selectDefaultAgentVersion(ctx);
        ensureAppFieldMappings(ctx);
        normalizeParamOutputMappings(ctx);
    }
    return { selectAgent };
}

function selectDefaultAgent(ctx) {
    if (!ctx.state.agents.value.length) {
        ctx.state.form.appId = '';
        ctx.state.form.appVersionId = '';
        ctx.state.form.appAgentAlias = '';
        return;
    }
    if (!ctx.state.agents.value.some((agent) => agent.id === ctx.state.form.appId)) {
        ctx.state.form.appId = ctx.state.agents.value[0].id;
        ctx.state.form.appAgentAlias = '';
    }
    selectDefaultAgentVersion(ctx);
    ensureAppFieldMappings(ctx);
}

function selectDefaultAgentVersion(ctx) {
    const versions = ctx.computed.agentVersions.value;
    if (!versions.length) {
        ctx.state.form.appVersionId = '';
        return;
    }
    if (!versions.some((version) => version.id === ctx.state.form.appVersionId)) {
        ctx.state.form.appVersionId = versions[0].id;
    }
}

function ensureAppFieldMappings(ctx) {
    if (ctx.state.form.appType !== 'agent') {
        clearAppFieldMappings(ctx);
        return;
    }
    const inputIds = new Set(ctx.computed.agentInputs.value.map((input) => input.id));
    Object.keys(ctx.state.appFieldMappings).forEach((key) => {
        if (!inputIds.has(key)) {
            delete ctx.state.appFieldMappings[key];
        }
    });
    ctx.computed.agentInputs.value.forEach((input) => {
        if (ctx.state.appFieldMappings[input.id] === undefined) {
            ctx.state.appFieldMappings[input.id] = findSuggestedDatasetField(ctx, input);
        }
    });
}

function clearAppFieldMappings(ctx) {
    Object.keys(ctx.state.appFieldMappings).forEach((key) => delete ctx.state.appFieldMappings[key]);
}

function findSuggestedDatasetField(ctx, input) {
    if (!ctx.state.fields.value.length) {
        return '';
    }
    const aliases = inputAliases(input.fieldName);
    const exactMatch = ctx.state.fields.value.find((field) => aliases.includes(normalizeFieldName(field.fieldName)));
    if (exactMatch?.id) {
        return exactMatch.id;
    }
    const fuzzyMatch = ctx.state.fields.value.find((field) => aliases.some((alias) => {
        const fieldName = normalizeFieldName(field.fieldName);
        return fieldName.includes(alias) || alias.includes(fieldName);
    }));
    return fuzzyMatch?.id || (ctx.state.fields.value.length === 1 ? ctx.state.fields.value[0].id || '' : '');
}

function inputAliases(fieldName) {
    const normalized = normalizeFieldName(fieldName);
    return normalized === 'query'
        ? ['query', 'question', 'input', 'prompt', '用户输入', '用户问题', '问题']
        : [normalized];
}

function normalizeFieldName(value) {
    return value.trim().toLowerCase();
}

function defaultAgentOutputs() {
    return [
        { id: 'text', fieldName: 'text', fieldType: 'string', description: '返回给用户的信息', displayOrder: 1 },
        { id: 'reasoning', fieldName: 'reasoning', fieldType: 'string', description: '智能体思考过程', displayOrder: 2 },
        { id: 'debug', fieldName: 'debug', fieldType: 'string', description: '智能体调试信息', displayOrder: 3 },
        { id: 'error', fieldName: 'error', fieldType: 'string', description: '智能体错误信息', displayOrder: 4 },
        { id: 'rawText', fieldName: 'rawText', fieldType: 'string', description: '消息合并后的原始文本', displayOrder: 5 },
        { id: 'skillTrigger', fieldName: 'skillTrigger', fieldType: 'string', description: '触发技能信息', displayOrder: 6 },
        { id: 'references', fieldName: 'references', fieldType: 'string', description: '引用来源列表', displayOrder: 7 },
        { id: 'toolCall', fieldName: 'toolCall', fieldType: 'string', description: '工具调用信息', displayOrder: 8 },
        { id: 'toolResponse', fieldName: 'toolResponse', fieldType: 'string', description: '工具响应信息', displayOrder: 9 },
        { id: 'genUi', fieldName: 'genUi', fieldType: 'string', description: '生成式UI信息', displayOrder: 10 }
    ];
}

function defaultAppOutputName(ctx) {
    return ctx.computed.agentOutputs.value[0]?.fieldName || 'text';
}

function normalizeParamOutputMappings(ctx) {
    ctx.state.evaluatorBlocks.value.forEach((block) => {
        Object.values(block.paramMappings).forEach((mapping) => normalizeMapping(ctx, mapping));
    });
}

function normalizeMapping(ctx, mapping) {
    if (mapping.sourceType !== 'app_output')
        return;
    if (ctx.state.form.appType !== 'agent') {
        mapping.sourceType = 'dataset_field';
        mapping.appOutputName = '';
    }
    else if (!mapping.appOutputName) {
        mapping.appOutputName = defaultAppOutputName(ctx);
    }
}

function createTagActions(ctx, loadActions) {
    async function openTagDrawer() {
        ctx.state.tagDrawerVisible.value = true;
        await loadActions.ensureTagsLoaded();
    }
    function addTag(tag) {
        if (isTagSelected(tag.id))
            return;
        if (ctx.state.selectedTagIds.value.length >= 5) {
            ElMessage.warning('标签最多添加5个');
            return;
        }
        ctx.state.selectedTagIds.value.push(tag.id);
    }
    function removeTag(tagId) {
        ctx.state.selectedTagIds.value = ctx.state.selectedTagIds.value.filter((item) => item !== tagId);
    }
    function isTagSelected(tagId) {
        return ctx.state.selectedTagIds.value.includes(tagId);
    }
    return { openTagDrawer, addTag, removeTag, isTagSelected };
}

function createSubmitActions(ctx) {
    async function submit() {
        if (!validate(ctx)) {
            return;
        } else {
            ctx.state.saving.value = true;
            try {
                const name = ctx.state.form.taskName.trim();
                const page = await taskApi.listTasks({ page: 1, size: 100, keyword: name });
                if (page.records.some((task) => task.base.taskName === name)) {
                    throw new Error('当前空间已存在同名评测任务');
                } else {
                    const created = await taskApi.createTask(taskPayload(ctx));
                    ElMessage.success('评测任务已创建');
                    await ctx.router.replace({ name: 'task-detail', params: { taskId: created.base.id } });
                }
            }
            catch (error) {
                ElMessage.error(getErrorMessage(error, '创建评测任务失败'));
            }
            finally {
                ctx.state.saving.value = false;
            }
        }
    }
    return { submit };
}

function taskPayload(ctx) {
    return {
        taskName: ctx.state.form.taskName.trim(),
        description: ctx.state.form.description.trim(),
        datasetId: ctx.state.form.datasetId,
        datasetVersionId: ctx.state.form.datasetVersionId,
        appType: ctx.state.form.appType,
        appId: ctx.state.form.appType === 'agent' ? ctx.state.form.appId : '',
        appVersionId: ctx.state.form.appType === 'agent' ? ctx.state.form.appVersionId : '',
        appAgentAlias: ctx.state.form.appType === 'agent' ? ctx.state.form.appAgentAlias : '',
        appFieldMappings: toAppFieldMappingPayload(ctx),
        evaluators: ctx.state.evaluatorBlocks.value.map((block) => toEvaluatorPayload(ctx, block)),
        tagIds: ctx.state.selectedTagIds.value
    };
}

function toAppFieldMappingPayload(ctx) {
    if (ctx.state.form.appType !== 'agent') {
        return [];
    }
    return ctx.computed.agentInputs.value.map((input) => ({
        appInputId: input.id,
        appInputName: input.fieldName,
        appInputType: input.fieldType || 'string',
        datasetFieldId: ctx.state.appFieldMappings[input.id] || ''
    }));
}

function toEvaluatorPayload(ctx, block) {
    return {
        evaluatorSource: block.evaluatorSource,
        evaluatorId: block.evaluatorId,
        evaluatorVersionId: block.evaluatorSource === 'custom' ? block.evaluatorVersionId : '',
        modelId: block.evaluatorSource === 'preset' ? block.modelId : '',
        modelName: block.evaluatorSource === 'preset' ? selectedModelName(ctx, block.modelId) : '',
        paramMappings: block.params.map((param) => toParamMappingPayload(ctx, block, param)).filter(Boolean)
    };
}

function toParamMappingPayload(ctx, block, param) {
    const mapping = block.paramMappings[paramKey(param)];
    if (!mapping || !shouldSubmitParamMapping(ctx, param, mapping)) {
        return null;
    }
    return {
        paramId: param.id,
        paramName: param.paramName,
        sourceType: mapping.sourceType,
        datasetFieldId: mapping.sourceType === 'dataset_field' ? mapping.datasetFieldId : '',
        appOutputName: mapping.sourceType === 'app_output' ? mapping.appOutputName : ''
    };
}

function selectedModelName(ctx, modelId) {
    return ctx.state.models.value.find((model) => model.modelId === modelId)?.modelName || '';
}

function shouldSubmitParamMapping(ctx, param, mapping) {
    if (param.required) {
        return true;
    }
    if (mapping.sourceType === 'dataset_field') {
        return Boolean(mapping.datasetFieldId);
    }
    return ctx.state.form.appType === 'agent' && Boolean(mapping.appOutputName);
}

function validate(ctx) {
    return validateBase(ctx) && validateAgent(ctx) && validateDimensions(ctx) && validateEvaluators(ctx);
}

function validateBase(ctx) {
    if (!ctx.state.form.taskName.trim()) {
        ElMessage.warning('请输入任务名称');
        return false;
    }
    if (!ctx.state.form.datasetId || !ctx.state.form.datasetVersionId) {
        ElMessage.warning('请选择评测集及版本');
        return false;
    }
    if (!ctx.computed.selectedVersion.value || ctx.computed.selectedVersion.value.draft) {
        ElMessage.warning('请选择已发布的评测集版本');
        return false;
    }
    return true;
}

function validateAgent(ctx) {
    if (ctx.state.form.appType !== 'agent') {
        return true;
    }
    if (!ctx.state.form.appId || !ctx.state.form.appVersionId) {
        ElMessage.warning('请选择智能体应用及版本');
        return false;
    }
    if (!ctx.computed.agentInputs.value.length) {
        ElMessage.warning('当前智能体暂无输入定义，不能创建关联应用任务');
        return false;
    }
    const missingInput = ctx.computed.agentInputs.value.find((input) => !ctx.state.appFieldMappings[input.id]);
    if (missingInput) {
        ElMessage.warning(`请选择智能体入参映射：${missingInput.fieldName}`);
        return false;
    }
    return true;
}

function validateDimensions(ctx) {
    if (!ctx.state.evaluatorBlocks.value.length && !ctx.state.selectedTagIds.value.length) {
        ElMessage.warning('请至少添加一个评估器或标签');
        return false;
    }
    if (ctx.state.evaluatorBlocks.value.length > 5) {
        ElMessage.warning('评估器最多添加5个');
        return false;
    }
    if (ctx.state.selectedTagIds.value.length > 5) {
        ElMessage.warning('标签最多添加5个');
        return false;
    }
    return true;
}

function validateEvaluators(ctx) {
    return ctx.state.evaluatorBlocks.value.every((block) => validateEvaluator(ctx, block));
}

function validateEvaluator(ctx, block) {
    if (!block.evaluatorId || (block.evaluatorSource === 'custom' && !block.evaluatorVersionId)) {
        ElMessage.warning('请选择评估器及版本');
        return false;
    }
    if (block.evaluatorType === 'code') {
        ElMessage.warning('暂不支持Code型评估器');
        return false;
    }
    if (!validatePresetModel(ctx, block)) {
        return false;
    }
    const missingParam = block.params.find((param) => param.required && !isParamMapped(ctx, block, param));
    if (missingParam) {
        ElMessage.warning(`请完成字段映射：${missingParam.paramName}`);
        return false;
    }
    return true;
}

function validatePresetModel(ctx, block) {
    if (block.evaluatorSource !== 'preset' || block.evaluatorType !== 'llm') {
        return true;
    }
    if (!block.modelId) {
        ElMessage.warning(`请选择预置评估器模型：${block.evaluatorName || block.evaluatorId}`);
        return false;
    }
    if (!selectedModelName(ctx, block.modelId)) {
        ElMessage.warning(`请选择预置评估器模型名称：${block.evaluatorName || block.evaluatorId}`);
        return false;
    }
    return true;
}

function isParamMapped(ctx, block, param) {
    const mapping = block.paramMappings[paramKey(param)];
    if (!mapping)
        return false;
    if (mapping.sourceType === 'dataset_field') {
        return Boolean(mapping.datasetFieldId);
    }
    return ctx.state.form.appType === 'agent' && Boolean(mapping.appOutputName);
}

function paramKey(param) {
    return param.id ? `id:${param.id}` : `name:${param.paramName}`;
}

function fieldTypeLabel(value) {
    if (value === 'number')
        return '数字';
    if (value === 'boolean')
        return '布尔';
    return '文本';
}

export function useTaskCreate() {
    const router = useRouter();
    const state = createState();
    const computedValues = createComputedValues(state);
    const ctx = { router, state, computed: computedValues };
    const loadActions = createLoadActions(ctx);
    const visibleHandlers = createVisibleHandlers(loadActions);
    const evaluatorActions = createEvaluatorActions(ctx, loadActions);
    const appActions = createAppActions(ctx, loadActions);
    const tagActions = createTagActions(ctx, loadActions);
    const submitActions = createSubmitActions(ctx);

    watch(() => state.form.datasetId, async () => {
        await loadActions.loadVersions();
    });
    watch(() => state.form.datasetVersionId, async () => {
        await loadActions.loadFields();
    });
    watch(() => state.form.appType, async () => {
        await handleAppTypeChanged(ctx, loadActions);
    });
    watch(() => state.form.appId, async (agentId) => {
        await appActions.selectAgent(agentId);
    });

    function backToList() {
        router.push({ name: 'tasks' });
    }

    return {
        ...state,
        ...computedValues,
        ...loadActions,
        ...visibleHandlers,
        ...evaluatorActions,
        ...tagActions,
        ...submitActions,
        paramKey,
        fieldTypeLabel,
        tagTypeLabel,
        backToList
    };
}

async function handleAppTypeChanged(ctx, loadActions) {
    if (ctx.state.form.appType === 'agent') {
        await loadActions.ensureAgentsLoaded();
        selectDefaultAgent(ctx);
    }
    else {
        ctx.state.form.appId = '';
        ctx.state.form.appVersionId = '';
        ctx.state.form.appAgentAlias = '';
        clearAppFieldMappings(ctx);
    }
    normalizeParamOutputMappings(ctx);
}
