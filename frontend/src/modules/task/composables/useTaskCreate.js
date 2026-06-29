import { computed, reactive, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { datasetApi } from '../../../api/dataset';
import { evaluatorApi } from '../../../api/evaluator';
import { integrationApi } from '../../../api/integration';
import { tagApi } from '../../../api/tag';
import { taskApi } from '../../../api/task';
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
export function useTaskCreate() {
    const router = useRouter();
    const loading = ref(false);
    const saving = ref(false);
    const tagDrawerVisible = ref(false);
    const tagKeyword = ref('');
    const tagTypeFilter = ref('');
    const datasets = ref([]);
    const versions = ref([]);
    const fields = ref([]);
    const tags = ref([]);
    const selectedTagIds = ref([]);
    const customEvaluators = ref([]);
    const presetCategories = ref([]);
    const evaluatorBlocks = ref([]);
    const agents = ref([]);
    const agentDetails = reactive({});
    const agentBundleVersions = reactive({});
    const models = ref([]);
    const appFieldMappings = reactive({});
    const datasetsLoaded = ref(false);
    const tagsLoaded = ref(false);
    const customEvaluatorsLoaded = ref(false);
    const presetCategoriesLoaded = ref(false);
    const agentsLoaded = ref(false);
    const agentDetailLoading = ref(false);
    const agentVersionLoading = ref(false);
    const modelsLoaded = ref(false);
    const modelLoading = ref(false);
    const form = reactive({
        taskName: '',
        description: '',
        datasetId: '',
        datasetVersionId: '',
        appType: 'none',
        appId: '',
        appVersionId: '',
        appAgentAlias: ''
    });
    const selectedVersion = computed(() => versions.value.find((item) => item.id === form.datasetVersionId));
    const publishedVersions = computed(() => versions.value.filter((item) => !item.draft).sort((a, b) => b.versionNo - a.versionNo));
    const canSubmit = computed(() => Boolean(form.taskName.trim()
        && form.datasetVersionId
        && (evaluatorBlocks.value.length || selectedTagIds.value.length)));
    const categoryOptions = computed(() => [{ id: '', categoryName: '全部分类', displayOrder: 0 }, ...presetCategories.value]);
    const selectedAgent = computed(() => agentDetails[form.appId] || agents.value.find((agent) => agent.id === form.appId));
    const agentVersions = computed(() => form.appId ? agentBundleVersions[form.appId] || [] : []);
    const agentChildAgents = computed(() => selectedAgent.value?.childAgents || []);
    const agentInputs = computed(() => selectedAgent.value?.inputs || []);
    const agentOutputs = computed(() => selectedAgent.value?.outputs || defaultAgentOutputs());
    const tagTypeOptions = computed(() => [
        { value: 'category', label: '分类' },
        { value: 'boolean', label: '布尔' },
        { value: 'number', label: '数字' },
        { value: 'text', label: '文本' }
    ]);
    const selectedTags = computed(() => selectedTagIds.value
        .map((tagId) => tags.value.find((tag) => tag.id === tagId))
        .filter((tag) => Boolean(tag)));
    const filteredTags = computed(() => {
        const keyword = tagKeyword.value.trim().toLowerCase();
        return tags.value.filter((tag) => {
            const matchesType = !tagTypeFilter.value || tag.tagType === tagTypeFilter.value;
            const matchesKeyword = !keyword
                || tag.tagName.toLowerCase().includes(keyword)
                || tag.description.toLowerCase().includes(keyword);
            return matchesType && matchesKeyword;
        });
    });
    watch(() => form.datasetId, async () => {
        await loadVersions();
    });
    watch(() => form.datasetVersionId, async () => {
        await loadFields();
    });
    watch(() => form.appType, async () => {
        if (form.appType === 'agent') {
            await ensureAgentsLoaded();
            selectDefaultAgent();
        }
        else {
            form.appId = '';
            form.appVersionId = '';
            form.appAgentAlias = '';
            clearAppFieldMappings();
        }
        normalizeParamOutputMappings();
    });
    watch(() => form.appId, async (agentId) => {
        form.appAgentAlias = '';
        form.appVersionId = '';
        if (agentId) {
            await Promise.all([
                ensureAgentDetailLoaded(agentId),
                ensureAgentBundlesLoaded(agentId)
            ]);
        }
        selectDefaultAgentVersion();
        ensureAppFieldMappings();
        normalizeParamOutputMappings();
    });
    async function ensureDatasetsLoaded() {
        if (datasetsLoaded.value)
            return;
        await withPageLoading(async () => {
            await loadDatasets();
            datasetsLoaded.value = true;
        }, '获取评测集失败');
    }
    async function ensureTagsLoaded() {
        if (tagsLoaded.value)
            return;
        await withPageLoading(async () => {
            await loadTags();
            tagsLoaded.value = true;
        }, '获取标签失败');
    }
    async function ensureCustomEvaluatorsLoaded() {
        if (customEvaluatorsLoaded.value)
            return;
        await withPageLoading(async () => {
            await loadCustomEvaluators();
            customEvaluatorsLoaded.value = true;
        }, '获取自定义评估器失败');
    }
    async function ensurePresetCategoriesLoaded() {
        if (presetCategoriesLoaded.value)
            return;
        await withPageLoading(async () => {
            await loadPresetCategories();
            presetCategoriesLoaded.value = true;
        }, '获取预置评估器分类失败');
    }
    async function ensureAgentsLoaded() {
        if (agentsLoaded.value)
            return;
        await withPageLoading(async () => {
            await loadAgents();
            agentsLoaded.value = true;
        }, '获取智能体列表失败');
    }
    async function ensureAgentDetailLoaded(agentId) {
        if (!agentId || agentDetails[agentId])
            return;
        agentDetailLoading.value = true;
        try {
            agentDetails[agentId] = await integrationApi.getAgentDetail(agentId);
        }
        catch (error) {
            ElMessage.error(error instanceof Error ? error.message : '获取智能体详情失败');
        }
        finally {
            agentDetailLoading.value = false;
        }
    }
    async function ensureAgentBundlesLoaded(agentId) {
        if (!agentId || agentBundleVersions[agentId])
            return;
        agentVersionLoading.value = true;
        try {
            agentBundleVersions[agentId] = await integrationApi.listAgentBundles(agentId);
        }
        catch (error) {
            ElMessage.error(error instanceof Error ? error.message : '获取智能体快照失败');
        }
        finally {
            agentVersionLoading.value = false;
        }
    }
    async function ensureModelsLoaded() {
        if (modelsLoaded.value || modelLoading.value)
            return;
        modelLoading.value = true;
        try {
            models.value = await integrationApi.listModels();
            modelsLoaded.value = true;
        }
        catch (error) {
            ElMessage.error(errorMessage(error, '获取模型列表失败'));
        }
        finally {
            modelLoading.value = false;
        }
    }
    async function ensurePresetOptionsLoaded(block, force = false) {
        if (!force && block.presetOptionsLoaded)
            return;
        block.loading = true;
        try {
            await loadPresetOptions(block);
            block.presetOptionsLoaded = true;
        }
        catch (error) {
            ElMessage.error(errorMessage(error, '获取预置评估器失败'));
        }
        finally {
            block.loading = false;
        }
    }
    async function withPageLoading(action, fallback) {
        loading.value = true;
        try {
            await action();
        }
        catch (error) {
            ElMessage.error(errorMessage(error, fallback));
        }
        finally {
            loading.value = false;
        }
    }
    function handleDatasetVisible(visible) {
        if (visible) {
            ensureDatasetsLoaded();
        }
    }
    function handleAgentVisible(visible) {
        if (visible) {
            ensureAgentsLoaded();
        }
    }
    function handleCustomEvaluatorVisible(visible) {
        if (visible) {
            ensureCustomEvaluatorsLoaded();
        }
    }
    function handlePresetCategoryVisible(visible) {
        if (visible) {
            ensurePresetCategoriesLoaded();
        }
    }
    function handlePresetEvaluatorVisible(block, visible) {
        if (visible) {
            ensurePresetOptionsLoaded(block);
        }
    }
    function handleModelVisible(visible) {
        if (visible) {
            ensureModelsLoaded();
        }
    }
    async function loadDatasets() {
        const page = await datasetApi.listDatasets({ page: 1, size: 100 });
        datasets.value = page.records;
    }
    async function loadVersions() {
        versions.value = [];
        fields.value = [];
        form.datasetVersionId = '';
        if (!form.datasetId)
            return;
        versions.value = await datasetApi.listVersions(form.datasetId);
        const latest = publishedVersions.value[0];
        if (latest) {
            form.datasetVersionId = latest.id;
        }
    }
    async function loadFields() {
        fields.value = [];
        if (!form.datasetVersionId)
            return;
        const detail = await datasetApi.getVersionDetail(form.datasetVersionId, { page: 1, size: 1 });
        fields.value = detail.fields;
        for (const block of evaluatorBlocks.value) {
            ensureParamMappings(block);
        }
        ensureAppFieldMappings();
    }
    async function loadTags() {
        const page = await tagApi.listTags({ page: 1, size: 100 });
        tags.value = page.records;
    }
    async function loadCustomEvaluators() {
        const page = await evaluatorApi.listEvaluators({ page: 1, size: 100 });
        customEvaluators.value = page.records;
    }
    async function loadPresetCategories() {
        presetCategories.value = await evaluatorApi.listPresetCategories();
    }
    async function loadAgents() {
        agents.value = await integrationApi.listAgents();
        if (form.appType === 'agent') {
            selectDefaultAgent();
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
    async function changePresetCategory(block) {
        block.evaluatorId = '';
        clearEvaluatorDetail(block);
        block.presetOptions = [];
        block.presetOptionsLoaded = false;
        await ensurePresetOptionsLoaded(block, true);
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
            if (block.evaluatorSource === 'preset') {
                const detail = await evaluatorApi.getPresetEvaluator(block.evaluatorId);
                fillFromPreset(block, detail);
            }
            else {
                block.versions = await evaluatorApi.listVersions(block.evaluatorId);
                const latest = [...block.versions].sort((a, b) => b.versionNo - a.versionNo)[0];
                block.evaluatorVersionId = latest?.id || '';
                if (block.evaluatorVersionId) {
                    await selectCustomVersion(block);
                }
            }
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
            fillFromCustom(block, detail);
        }
        finally {
            block.loading = false;
        }
    }
    function fillFromPreset(block, detail) {
        block.evaluatorName = detail.evaluatorName;
        block.evaluatorType = detail.evaluatorType;
        block.description = detail.description;
        block.versionName = '预置';
        block.scoreMin = Number(detail.scoreMin);
        block.scoreMax = Number(detail.scoreMax);
        block.passThreshold = Number(detail.passThreshold);
        block.prompt = detail.prompt;
        block.executeCode = detail.executeCode;
        block.modelId = detail.modelId || '';
        block.params = detail.params;
        ensureParamMappings(block);
    }
    function fillFromCustom(block, detail) {
        block.evaluatorName = detail.evaluatorName;
        block.evaluatorType = detail.evaluatorType;
        block.description = detail.description;
        block.versionName = detail.versionName;
        block.scoreMin = Number(detail.scoreMin);
        block.scoreMax = Number(detail.scoreMax);
        block.passThreshold = Number(detail.passThreshold);
        block.prompt = detail.prompt;
        block.executeCode = detail.executeCode;
        block.modelId = '';
        block.params = detail.params;
        ensureParamMappings(block);
    }
    function clearEvaluatorDetail(block) {
        block.evaluatorName = '';
        block.evaluatorType = '';
        block.description = '';
        block.versionName = '';
        block.modelId = '';
        block.prompt = '';
        block.executeCode = '';
        block.scoreMin = undefined;
        block.scoreMax = undefined;
        block.passThreshold = undefined;
        block.params = [];
        block.paramMappings = {};
        block.versions = [];
        if (block.evaluatorSource !== 'preset') {
            block.presetOptions = [];
            block.presetOptionsLoaded = false;
        }
    }
    function ensureParamMappings(block) {
        const next = {};
        for (const param of block.params) {
            const key = paramKey(param);
            next[key] = block.paramMappings[key] || {
                sourceType: 'dataset_field',
                datasetFieldId: '',
                appOutputName: defaultAppOutputName()
            };
        }
        block.paramMappings = next;
    }
    function selectDefaultAgent() {
        if (!agents.value.length) {
            form.appId = '';
            form.appVersionId = '';
            form.appAgentAlias = '';
            return;
        }
        const exists = agents.value.some((agent) => agent.id === form.appId);
        if (!exists) {
            form.appId = agents.value[0].id;
            form.appAgentAlias = '';
        }
        selectDefaultAgentVersion();
        ensureAppFieldMappings();
    }
    function selectDefaultAgentVersion() {
        const versions = agentVersions.value;
        if (!versions.length) {
            form.appVersionId = '';
            return;
        }
        const exists = versions.some((version) => version.id === form.appVersionId);
        if (!exists) {
            form.appVersionId = versions[0].id;
        }
    }
    function ensureAppFieldMappings() {
        if (form.appType !== 'agent') {
            clearAppFieldMappings();
            return;
        }
        const inputIds = new Set(agentInputs.value.map((input) => input.id));
        for (const key of Object.keys(appFieldMappings)) {
            if (!inputIds.has(key)) {
                delete appFieldMappings[key];
            }
        }
        for (const input of agentInputs.value) {
            if (appFieldMappings[input.id] === undefined) {
                appFieldMappings[input.id] = findSuggestedDatasetField(input);
            }
        }
    }
    function clearAppFieldMappings() {
        for (const key of Object.keys(appFieldMappings)) {
            delete appFieldMappings[key];
        }
    }
    function findSuggestedDatasetField(input) {
        if (!fields.value.length) {
            return '';
        }
        const aliases = inputAliases(input.fieldName);
        const exactMatch = fields.value.find((field) => aliases.includes(normalizeFieldName(field.fieldName)));
        if (exactMatch?.id) {
            return exactMatch.id;
        }
        const fuzzyMatch = fields.value.find((field) => {
            const fieldName = normalizeFieldName(field.fieldName);
            return aliases.some((alias) => fieldName.includes(alias) || alias.includes(fieldName));
        });
        if (fuzzyMatch?.id) {
            return fuzzyMatch.id;
        }
        return fields.value.length === 1 ? fields.value[0].id || '' : '';
    }
    function inputAliases(fieldName) {
        const normalized = normalizeFieldName(fieldName);
        if (normalized === 'query') {
            return ['query', 'question', 'input', 'prompt', '用户输入', '用户问题', '问题'];
        }
        return [normalized];
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
            { id: 'genUi', fieldName: 'genUi', fieldType: 'string', description: '生成式 UI 信息', displayOrder: 10 }
        ];
    }
    function defaultAppOutputName() {
        return agentOutputs.value[0]?.fieldName || 'text';
    }
    function normalizeParamOutputMappings() {
        for (const block of evaluatorBlocks.value) {
            for (const mapping of Object.values(block.paramMappings)) {
                if (mapping.sourceType === 'app_output') {
                    if (form.appType !== 'agent') {
                        mapping.sourceType = 'dataset_field';
                        mapping.appOutputName = '';
                    }
                    else if (!mapping.appOutputName) {
                        mapping.appOutputName = defaultAppOutputName();
                    }
                }
            }
        }
    }
    function addEvaluator() {
        if (evaluatorBlocks.value.length >= 5) {
            ElMessage.warning('评估器最多添加5个');
            return;
        }
        const block = createEvaluatorBlock();
        evaluatorBlocks.value.push(block);
    }
    function removeEvaluator(index) {
        evaluatorBlocks.value.splice(index, 1);
    }
    async function openTagDrawer() {
        tagDrawerVisible.value = true;
        await ensureTagsLoaded();
    }
    function addTag(tag) {
        if (isTagSelected(tag.id))
            return;
        if (selectedTagIds.value.length >= 5) {
            ElMessage.warning('标签最多添加5个');
            return;
        }
        selectedTagIds.value.push(tag.id);
    }
    function removeTag(tagId) {
        selectedTagIds.value = selectedTagIds.value.filter((item) => item !== tagId);
    }
    function isTagSelected(tagId) {
        return selectedTagIds.value.includes(tagId);
    }
    async function submit() {
        if (!validate())
            return;
        saving.value = true;
        try {
            const created = await taskApi.createTask({
                taskName: form.taskName.trim(),
                description: form.description.trim(),
                datasetId: form.datasetId,
                datasetVersionId: form.datasetVersionId,
                appType: form.appType,
                appId: form.appType === 'agent' ? form.appId : '',
                appVersionId: form.appType === 'agent' ? form.appVersionId : '',
                appAgentAlias: form.appType === 'agent' ? form.appAgentAlias : '',
                appFieldMappings: toAppFieldMappingPayload(),
                evaluators: evaluatorBlocks.value.map(toEvaluatorPayload),
                tagIds: selectedTagIds.value
            });
            ElMessage.success('评测任务已创建');
            await router.replace({ name: 'task-detail', params: { taskId: created.base.id } });
        }
        catch (error) {
            ElMessage.error(error instanceof Error ? error.message : '创建评测任务失败');
        }
        finally {
            saving.value = false;
        }
    }
    function toAppFieldMappingPayload() {
        if (form.appType !== 'agent') {
            return [];
        }
        return agentInputs.value.map((input) => ({
            appInputId: input.id,
            appInputName: input.fieldName,
            appInputType: input.fieldType || 'string',
            datasetFieldId: appFieldMappings[input.id] || ''
        }));
    }
    function toEvaluatorPayload(block) {
        return {
            evaluatorSource: block.evaluatorSource,
            evaluatorId: block.evaluatorId,
            evaluatorVersionId: block.evaluatorSource === 'custom' ? block.evaluatorVersionId : '',
            modelId: block.evaluatorSource === 'preset' ? block.modelId : '',
            paramMappings: block.params
                .map((param) => {
                const mapping = block.paramMappings[paramKey(param)];
                return mapping && shouldSubmitParamMapping(param, mapping)
                    ? {
                        paramId: param.id,
                        paramName: param.paramName,
                        sourceType: mapping.sourceType,
                        datasetFieldId: mapping.sourceType === 'dataset_field' ? mapping.datasetFieldId : '',
                        appOutputName: mapping.sourceType === 'app_output' ? mapping.appOutputName : ''
                    }
                    : null;
            })
                .filter((mapping) => Boolean(mapping))
        };
    }
    function shouldSubmitParamMapping(param, mapping) {
        if (param.required) {
            return true;
        }
        if (mapping.sourceType === 'dataset_field') {
            return Boolean(mapping.datasetFieldId);
        }
        return form.appType === 'agent' && Boolean(mapping.appOutputName);
    }
    function validate() {
        if (!form.taskName.trim()) {
            ElMessage.warning('请输入任务名称');
            return false;
        }
        if (!form.datasetId || !form.datasetVersionId) {
            ElMessage.warning('请选择评测集及版本');
            return false;
        }
        if (!selectedVersion.value || selectedVersion.value.draft) {
            ElMessage.warning('请选择已发布的评测集版本');
            return false;
        }
        if (form.appType === 'agent') {
            if (!form.appId || !form.appVersionId) {
                ElMessage.warning('请选择智能体应用及版本');
                return false;
            }
            if (!agentInputs.value.length) {
                ElMessage.warning('当前智能体暂无输入定义，不能创建关联应用任务');
                return false;
            }
            for (const input of agentInputs.value) {
                if (!appFieldMappings[input.id]) {
                    ElMessage.warning(`请选择智能体入参映射：${input.fieldName}`);
                    return false;
                }
            }
        }
        if (!evaluatorBlocks.value.length && !selectedTagIds.value.length) {
            ElMessage.warning('请至少添加一个评估器或标签');
            return false;
        }
        if (evaluatorBlocks.value.length > 5) {
            ElMessage.warning('评估器最多添加5个');
            return false;
        }
        if (selectedTagIds.value.length > 5) {
            ElMessage.warning('标签最多添加5个');
            return false;
        }
        for (const block of evaluatorBlocks.value) {
            if (!block.evaluatorId || (block.evaluatorSource === 'custom' && !block.evaluatorVersionId)) {
                ElMessage.warning('请选择评估器及版本');
                return false;
            }
            if (block.evaluatorSource === 'preset' && block.evaluatorType === 'llm' && !block.modelId) {
                ElMessage.warning(`请选择预置评估器模型：${block.evaluatorName || block.evaluatorId}`);
                return false;
            }
            for (const param of block.params) {
                if (!param.required)
                    continue;
                const mapping = block.paramMappings[paramKey(param)];
                if (!mapping) {
                    ElMessage.warning(`请完成字段映射：${param.paramName}`);
                    return false;
                }
                if (mapping.sourceType === 'dataset_field' && !mapping.datasetFieldId) {
                    ElMessage.warning(`请选择评测集字段：${param.paramName}`);
                    return false;
                }
                if (mapping.sourceType === 'app_output' && form.appType !== 'agent') {
                    ElMessage.warning('未关联应用时不能映射到应用输出');
                    return false;
                }
            }
        }
        return true;
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
    function tagTypeLabel(value) {
        const map = {
            category: '分类',
            boolean: '布尔',
            number: '数字',
            text: '文本'
        };
        return value ? map[value] || value : '-';
    }
    function errorMessage(error, fallback) {
        return error instanceof Error && error.message ? error.message : fallback;
    }
    function backToList() {
        router.push({ name: 'tasks' });
    }
    return {
        loading,
        saving,
        tagDrawerVisible,
        tagKeyword,
        tagTypeFilter,
        datasets,
        versions,
        publishedVersions,
        fields,
        tags,
        selectedTagIds,
        selectedTags,
        filteredTags,
        tagTypeOptions,
        customEvaluators,
        presetCategories,
        categoryOptions,
        evaluatorBlocks,
        agents,
        models,
        agentDetailLoading,
        agentVersionLoading,
        modelLoading,
        selectedAgent,
        agentVersions,
        agentChildAgents,
        agentInputs,
        agentOutputs,
        appFieldMappings,
        form,
        canSubmit,
        ensureDatasetsLoaded,
        ensureTagsLoaded,
        ensureCustomEvaluatorsLoaded,
        ensurePresetCategoriesLoaded,
        ensureAgentsLoaded,
        ensurePresetOptionsLoaded,
        handleDatasetVisible,
        handleAgentVisible,
        handleCustomEvaluatorVisible,
        handlePresetCategoryVisible,
        handlePresetEvaluatorVisible,
        handleModelVisible,
        changePresetCategory,
        changeEvaluatorSource,
        selectEvaluator,
        selectCustomVersion,
        addEvaluator,
        removeEvaluator,
        openTagDrawer,
        addTag,
        removeTag,
        isTagSelected,
        submit,
        paramKey,
        fieldTypeLabel,
        tagTypeLabel,
        backToList
    };
}
