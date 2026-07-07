import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { evaluatorApi } from '../../../api/evaluator';
import { remoteCallApi } from '../../../api/remoteCall';
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
    const canEdit = computed(() => (!isEdit.value || Boolean(activeDetail.value?.draft)) && form.evaluatorType !== 'code');
    const pageTitle = computed(() => (isEdit.value ? form.evaluatorName || '编辑评估器' : '创建评估器'));
    const activeVersion = computed(() => versions.value.find((item) => item.id === activeVersionId.value));
    const promptParams = computed(() => (form.evaluatorType === 'llm' ? form.params : []));
    const modelOptions = computed(() => models.value.map((model) => ({
        label: model.name || model.modelName || model.modelId,
        value: model.modelId
    })));
    onMounted(async () => {
        if (isEdit.value) {
            await loadVersions();
        }
        else if (presetId.value) {
            await loadPreset(presetId.value);
        }
        else {
            syncPromptParams();
        }
    });
    async function loadModelOptions() {
        if (models.value.length || modelLoading.value) {
            return;
        }
        modelLoading.value = true;
        try {
            models.value = await remoteCallApi.listModels();
        }
        catch (error) {
            ElMessage.error(errorMessage(error, '获取模型列表失败'));
        }
        finally {
            modelLoading.value = false;
        }
    }
    function clearModelOptions() {
        models.value = [];
    }
    async function handleModelVisibleChange(visible) {
        if (visible) {
            await loadModelOptions();
        }
    }
    async function refreshEditor() {
        clearModelOptions();
        if (isEdit.value) {
            await loadVersions(activeVersionId.value);
        }
    }
    watch(() => [form.evaluatorType, form.prompt], () => {
        if (form.evaluatorType === 'llm') {
            syncPromptParams();
        }
    });
    async function loadPreset(id) {
        loading.value = true;
        try {
            const preset = await evaluatorApi.getPresetEvaluator(id);
            fillFromPreset(preset);
        }
        finally {
            loading.value = false;
        }
    }
    async function loadVersions(preferredVersionId) {
        if (!evaluatorId.value)
            return;
        loading.value = true;
        try {
            versions.value = await evaluatorApi.listVersions(evaluatorId.value);
            const preferred = versions.value.find((item) => item.id === preferredVersionId);
            const draft = versions.value.find((item) => item.draft);
            const fallback = preferred ?? draft ?? versions.value[versions.value.length - 1];
            if (fallback) {
                await selectVersion(fallback.id);
            }
        }
        finally {
            loading.value = false;
        }
    }
    async function selectVersion(versionId) {
        activeVersionId.value = versionId;
        const detail = await evaluatorApi.getVersion(versionId);
        activeDetail.value = detail;
        fillFromConfig(detail);
    }
    function fillFromPreset(preset) {
        form.evaluatorName = preset.evaluatorName;
        form.description = preset.description;
        form.evaluatorType = preset.evaluatorType;
        form.modelId = preset.modelId || '';
        form.modelName = preset.modelName || '';
        form.prompt = preset.prompt;
        form.executeCode = preset.executeCode;
        form.scoreMin = Number(preset.scoreMin ?? 1);
        form.scoreMax = Number(preset.scoreMax ?? 5);
        form.passThreshold = Number(preset.passThreshold ?? 3);
        form.params = preset.params.map(cloneParam);
        if (form.evaluatorType === 'llm') {
            syncPromptParams();
        }
    }
    function fillFromConfig(config) {
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
        if (form.evaluatorType === 'llm') {
            syncPromptParams();
        }
        if (!form.params.length && form.evaluatorType === 'code') {
            form.params = defaultParams();
        }
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
    async function submit() {
        if (!validateForm())
            return;
        saving.value = true;
        try {
            if (isEdit.value) {
                if (!canEdit.value || !activeVersionId.value)
                    return;
                const saved = await evaluatorApi.updateDraft(activeVersionId.value, payload());
                activeDetail.value = saved;
                ElMessage.success('草稿已保存');
                await loadVersions(saved.versionId);
            }
            else {
                const created = await evaluatorApi.createEvaluator(payload());
                ElMessage.success('评估器已创建');
                await router.replace({ name: 'evaluator-edit', params: { evaluatorId: created.evaluatorId } });
                versions.value = [{
                        id: created.versionId,
                        evaluatorId: created.evaluatorId,
                        versionNo: created.versionNo,
                        versionName: created.versionName,
                        draft: created.draft,
                        createdDate: created.createdDate,
                        lastUpdatedDate: created.lastUpdatedDate
                    }];
                activeVersionId.value = created.versionId;
                activeDetail.value = created;
            }
        }
        catch (error) {
            ElMessage.error(errorMessage(error, isEdit.value ? '保存草稿失败' : '创建评估器失败'));
        }
        finally {
            saving.value = false;
        }
    }
    async function publishDraft() {
        if (!isEdit.value || !evaluatorId.value)
            return;
        if (!canEdit.value) {
            ElMessage.warning('当前版本不可发布');
            return;
        }
        await ElMessageBox.confirm('发布后将生成新的只读版本，确定发布当前草稿吗？', '发布版本', { type: 'success' });
        publishing.value = true;
        try {
            if (!validateForm()) {
                return;
            }
            await evaluatorApi.updateDraft(activeVersionId.value, payload());
            const published = await evaluatorApi.publish(evaluatorId.value);
            ElMessage.success(`已发布 ${published.versionName}`);
            await loadVersions(published.versionId);
        }
        catch (error) {
            ElMessage.error(errorMessage(error, '发布版本失败'));
        }
        finally {
            publishing.value = false;
        }
    }
    function payload() {
        const params = form.evaluatorType === 'llm' ? syncPromptParams() : form.params;
        return {
            evaluatorName: form.evaluatorName.trim(),
            evaluatorType: form.evaluatorType,
            description: form.description.trim(),
            modelId: form.evaluatorType === 'llm' ? form.modelId : '',
            modelName: form.evaluatorType === 'llm' ? selectedModelName() : '',
            prompt: form.evaluatorType === 'llm' ? form.prompt : '',
            executeCode: form.evaluatorType === 'code' ? form.executeCode : '',
            scoreMin: Number(form.scoreMin),
            scoreMax: Number(form.scoreMax),
            passThreshold: Number(form.passThreshold),
            params: params.map(toParamPayload)
        };
    }
    function selectedModelName() {
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
    function syncPromptParams() {
        const names = extractPromptParams(form.prompt);
        const mappedParams = new Map(form.params.map((param) => [param.paramName, param]));
        form.params = names.map((name, index) => {
            const existing = mappedParams.get(name);
            return {
                ...(existing ? cloneParam(existing) : createParam(name)),
                paramName: name,
                displayOrder: index + 1
            };
        });
        return form.params;
    }
    function validateForm() {
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
        if (form.evaluatorType === 'llm' && !form.prompt.trim()) {
            ElMessage.warning('请输入Prompt');
            return false;
        }
        if (form.evaluatorType === 'llm' && !form.modelId) {
            ElMessage.warning('请选择模型');
            return false;
        }
        if (form.evaluatorType === 'llm' && !selectedModelName()) {
            ElMessage.warning('请选择模型名称');
            return false;
        }
        if (form.evaluatorType === 'llm' && !extractPromptParams(form.prompt).length) {
            ElMessage.warning('Prompt至少需要包含一个${参数名}参数');
            return false;
        }
        if (form.evaluatorType === 'code') {
            ElMessage.warning('暂不支持Code型评估器');
            return false;
        }
        return true;
    }
    function switchType(type) {
        if (type === 'code') {
            return;
        }
        if (!canEdit.value || (isEdit.value && activeDetail.value?.evaluatorType !== type)) {
            return;
        }
        form.evaluatorType = type;
        if (type === 'llm') {
            syncPromptParams();
        }
    }
    function addParam() {
        form.params.push(createParam());
    }
    function removeParam(index) {
        form.params.splice(index, 1);
    }
    function changeParamType(index, dataType) {
        form.params[index].dataType = dataType;
    }
    function backToList() {
        router.push({ name: 'evaluators' });
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
    function formatTime(value) {
        if (!value)
            return '-';
        const numberValue = Number(value);
        if (Number.isNaN(numberValue))
            return value;
        return new Date(numberValue).toLocaleString();
    }
    function errorMessage(error, fallback) {
        return error instanceof Error && error.message ? error.message : fallback;
    }
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
        loadModelOptions,
        handleModelVisibleChange,
        refreshEditor,
        loadVersions,
        selectVersion,
        submit,
        publishDraft,
        switchType,
        addParam,
        removeParam,
        changeParamType,
        backToList,
        formatTime
    };
}
