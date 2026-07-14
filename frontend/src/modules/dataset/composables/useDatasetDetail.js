import { computed, reactive, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { datasetApi } from '../../../api/dataset';
import { getErrorMessage } from '../../../utils/composableHelpers';
import { formatDateTime } from '../../../utils/formatters';

function createState() {
    return {
        detailLoading: ref(false),
        datasetSummary: ref(),
        versions: ref([]),
        activeVersionId: ref(''),
        detail: ref(),
        tablePage: ref(1),
        tableSize: ref(10),
        searchFieldId: ref(''),
        searchKeyword: ref(''),
        fieldVisible: ref(false),
        rowVisible: ref(false),
        rowEditingId: ref(''),
        excelInput: ref(),
        coverExcelInput: ref(),
        draggedFieldIndex: ref(null),
        dragOverFieldIndex: ref(null),
        fieldForm: ref([]),
        rowForm: reactive({})
    };
}

function createComputed(state) {
    const activeVersion = computed(() => state.detail.value?.version);
    const fields = computed(() => state.detail.value?.fields ?? []);
    return {
        datasetTitle: computed(() => state.datasetSummary.value?.name || '评测集详情'),
        activeVersion,
        isDraft: computed(() => activeVersion.value?.draft === true),
        tableRows: computed(() => state.detail.value?.rows.records ?? []),
        tableTotal: computed(() => state.detail.value?.rows.total ?? 0),
        fields,
        dataTableKey: computed(() => fields.value.map((field) => `${field.id}:${field.fieldName}:${field.required}:${field.displayOrder}`).join('|'))
    };
}

function resetDrag(state) {
    state.draggedFieldIndex.value = null;
    state.dragOverFieldIndex.value = null;
}

function clearReactive(target) {
    Object.keys(target).forEach((key) => delete target[key]);
}

function requireActiveVersion(state) {
    return Boolean(state.activeVersionId.value);
}

function createVersionActions(ctx) {
    async function loadDataset() {
        ctx.state.detailLoading.value = true;
        try {
            await loadDatasetSummary();
            await loadVersions();
        }
        finally {
            ctx.state.detailLoading.value = false;
        }
    }
    async function loadDatasetSummary() {
        const page = await datasetApi.listDatasets({ page: 1, size: 100 });
        ctx.state.datasetSummary.value = page.records.find((item) => item.id === ctx.datasetId.value);
    }
    async function loadVersions(preferredVersionId) {
        ctx.state.versions.value = await datasetApi.listVersions(ctx.datasetId.value);
        const preferred = ctx.state.versions.value.find((item) => item.id === preferredVersionId);
        const fallback = preferred ?? ctx.state.versions.value.find((item) => item.draft) ?? ctx.state.versions.value[0];
        if (fallback) {
            await selectVersion(fallback.id);
            return;
        }
        ctx.state.activeVersionId.value = '';
        ctx.state.detail.value = undefined;
    }
    async function selectVersion(versionId) {
        ctx.state.activeVersionId.value = versionId;
        ctx.state.tablePage.value = 1;
        await loadDetail();
    }
    async function loadDetail() {
        if (!requireActiveVersion(ctx.state))
            return;
        ctx.state.detailLoading.value = true;
        try {
            ctx.state.detail.value = await datasetApi.getVersionDetail(ctx.state.activeVersionId.value, {
                page: ctx.state.tablePage.value,
                size: ctx.state.tableSize.value,
                fieldId: ctx.state.searchFieldId.value || undefined,
                keyword: ctx.state.searchKeyword.value || undefined
            });
        }
        finally {
            ctx.state.detailLoading.value = false;
        }
    }
    async function changeTableSize() {
        ctx.state.tablePage.value = 1;
        await loadDetail();
    }
    function backToList() {
        ctx.router.push({ name: 'datasets' });
    }
    async function publishDraft() {
        await ElMessageBox.confirm('发布后将生成新的只读版本，确定发布当前草稿吗？', '发布版本', { type: 'success' });
        const version = await datasetApi.publish(ctx.datasetId.value);
        ElMessage.success(`已发布${version.versionName}`);
        await loadDatasetSummary();
        await loadVersions(version.id);
    }
    async function removeVersion(version) {
        await ElMessageBox.confirm(`确定删除 ${version.versionName} 吗？`, '删除版本', { type: 'warning' });
        await datasetApi.deleteVersion(version.id);
        ElMessage.success('版本已删除');
        await loadDatasetSummary();
        await loadVersions();
    }
    async function coverDraft(version) {
        await ElMessageBox.confirm(`确定用 ${version.versionName} 全量覆盖草稿吗？`, '覆盖草稿', { type: 'warning' });
        const draft = await datasetApi.coverDraft(ctx.datasetId.value, version.id);
        ElMessage.success('草稿已覆盖');
        await loadVersions(draft.id);
    }
    return { loadDataset, loadDatasetSummary, loadVersions, selectVersion, loadDetail, changeTableSize, backToList, publishDraft, removeVersion, coverDraft };
}

function createFieldActions(ctx, versionActions) {
    function addField(target) {
        if (target.length >= 10) {
            ElMessage.warning('评测集最多支持10列');
            return;
        }
        target.push({ fieldName: '', fieldType: 'string', required: false, description: '' });
    }
    function removeField(target, index) {
        target.splice(index, 1);
    }
    function startFieldDrag(index) {
        ctx.state.draggedFieldIndex.value = index;
    }
    function enterFieldDrag(index) {
        if (ctx.state.draggedFieldIndex.value !== null && ctx.state.draggedFieldIndex.value !== index) {
            ctx.state.dragOverFieldIndex.value = index;
        }
    }
    function dropField(target, targetIndex) {
        const sourceIndex = ctx.state.draggedFieldIndex.value;
        if (sourceIndex !== null && sourceIndex !== targetIndex) {
            const [moved] = target.splice(sourceIndex, 1);
            target.splice(targetIndex, 0, moved);
        }
        resetDrag(ctx.state);
    }
    function endFieldDrag() {
        resetDrag(ctx.state);
    }
    function openFieldDialog() {
        ctx.state.fieldForm.value = ctx.computed.fields.value.map((field) => ({ ...field }));
        ctx.state.fieldVisible.value = true;
    }
    async function submitFields() {
        if (!requireActiveVersion(ctx.state))
            return;
        if (!ctx.state.fieldForm.value.length || ctx.state.fieldForm.value.some((field) => !field.fieldName.trim())) {
            ElMessage.warning('请完善列名');
            return;
        }
        const savedFields = await datasetApi.replaceFields(ctx.state.activeVersionId.value, ctx.state.fieldForm.value);
        if (ctx.state.detail.value) {
            ctx.state.detail.value = { ...ctx.state.detail.value, fields: savedFields };
        }
        ctx.state.fieldVisible.value = false;
        ElMessage.success('表头已保存');
        await versionActions.loadDetail();
    }
    return { addField, removeField, startFieldDrag, enterFieldDrag, dropField, endFieldDrag, openFieldDialog, submitFields };
}

function createExcelActions(ctx, versionActions) {
    function openExcelImport() {
        if (canImportExcel(ctx)) {
            ctx.state.excelInput.value?.click();
        }
    }
    async function openExcelCover() {
        if (!canImportExcel(ctx))
            return;
        await ElMessageBox.confirm('全量覆盖会清空草稿现有数据，并以Excel数据为准，确定继续吗？', '全量覆盖', { type: 'warning' });
        ctx.state.coverExcelInput.value?.click();
    }
    async function importExcel(event) {
        await submitExcel(event, (file) => datasetApi.importRows(ctx.state.activeVersionId.value, file), '已导入', '导入失败，请确认Excel包含所有必填列');
    }
    async function coverExcel(event) {
        await submitExcel(event, (file) => datasetApi.coverRowsByExcel(ctx.state.activeVersionId.value, file), '已覆盖导入', '覆盖失败，请确认Excel包含所有必填列');
    }
    async function submitExcel(event, action, successPrefix, fallback) {
        if (!requireActiveVersion(ctx.state))
            return;
        const input = event.target;
        const file = input.files?.[0];
        if (!file)
            return;
        try {
            const result = await action(file);
            ElMessage.success(`${successPrefix}${result.importedCount} 行`);
            await versionActions.loadDetail();
            await versionActions.loadDatasetSummary();
        }
        catch (error) {
            ElMessage.error(getErrorMessage(error, fallback));
        }
        finally {
            input.value = '';
        }
    }
    return { openExcelImport, openExcelCover, importExcel, coverExcel };
}

function canImportExcel(ctx) {
    if (!requireActiveVersion(ctx.state))
        return false;
    if (!ctx.computed.fields.value.length) {
        ElMessage.warning('请先维护表头');
        return false;
    }
    return true;
}

function createRowActions(ctx, versionActions, excelActions) {
    function openRowDialog(row) {
        ctx.state.rowEditingId.value = row?.id ?? '';
        clearReactive(ctx.state.rowForm);
        ctx.computed.fields.value.forEach((field) => {
            if (field.id) {
                ctx.state.rowForm[field.id] = row?.values[field.id] ?? '';
            }
        });
        ctx.state.rowVisible.value = true;
    }
    async function submitRow() {
        if (!requireActiveVersion(ctx.state))
            return;
        const missingField = ctx.computed.fields.value.find((field) => field.required && !String(ctx.state.rowForm[field.id || ''] ?? '').trim());
        if (missingField) {
            ElMessage.warning(`请填写${missingField.fieldName}`);
            return;
        }
        await saveRow();
        ctx.state.rowVisible.value = false;
        await versionActions.loadDetail();
        await versionActions.loadDatasetSummary();
    }
    async function saveRow() {
        if (ctx.state.rowEditingId.value) {
            await datasetApi.updateRow(ctx.state.activeVersionId.value, ctx.state.rowEditingId.value, { ...ctx.state.rowForm });
            ElMessage.success('数据已更新');
            return;
        }
        await datasetApi.addRow(ctx.state.activeVersionId.value, { ...ctx.state.rowForm });
        ElMessage.success('数据已新增');
    }
    async function removeRow(row) {
        if (!requireActiveVersion(ctx.state))
            return;
        await ElMessageBox.confirm('确定删除该条数据吗？', '删除数据', { type: 'warning' });
        await datasetApi.deleteRow(ctx.state.activeVersionId.value, row.id);
        ElMessage.success('已删除');
        await versionActions.loadDetail();
        await versionActions.loadDatasetSummary();
    }
    async function handleAddDataCommand(command) {
        if (command === 'single') {
            openRowDialog();
        }
        else if (command === 'import') {
            excelActions.openExcelImport();
        }
        else if (command === 'cover') {
            await excelActions.openExcelCover();
        }
    }
    return { openRowDialog, submitRow, removeRow, handleAddDataCommand };
}

export function useDatasetDetail(datasetId) {
    const router = useRouter();
    const state = createState();
    const computedValues = createComputed(state);
    const ctx = { router, datasetId, state, computed: computedValues };
    const versionActions = createVersionActions(ctx);
    const fieldActions = createFieldActions(ctx, versionActions);
    const excelActions = createExcelActions(ctx, versionActions);
    const rowActions = createRowActions(ctx, versionActions, excelActions);
    watch(datasetId, async () => {
        await versionActions.loadDataset();
    }, { immediate: true });

    return {
        ...state,
        ...computedValues,
        ...versionActions,
        ...fieldActions,
        ...rowActions,
        importExcel: excelActions.importExcel,
        coverExcel: excelActions.coverExcel,
        formatTime: formatDateTime
    };
}
