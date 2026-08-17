import { onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { datasetApi } from '../../../api/dataset';
import { useResourceDescriptionEdit } from '../../../composables/useResourceDescriptionEdit';
import { formatDateTime } from '../../../utils/formatters';
import { getErrorMessage, movePreviousPageIfLastRow, runExclusive, runExclusiveById, sortParams, toggleDescSort } from '../../../utils/composableHelpers';

function defaultFields() {
    return [
        { fieldName: 'query', fieldType: 'string', required: true, description: '用户问题' },
        { fieldName: 'reference_response', fieldType: 'string', required: false, description: '参考答案' }
    ];
}

function createFieldDragState() {
    return { draggedFieldIndex: ref(null), dragOverFieldIndex: ref(null) };
}

function addField(target) {
    if (target.length >= 10) {
        ElMessage.warning('评测集最多支持10列');
        return;
    } else if (target.some((field) => !field.fieldName.trim() || !field.fieldType)) {
        ElMessage.warning('请先完善已有列的列名和类型');
        return;
    } else {
        target.push({ fieldName: '', fieldType: 'string', required: false, description: '' });
    }
}

function removeField(target, index) {
    target.splice(index, 1);
}

function createDragActions(drag) {
    function startFieldDrag(index) {
        drag.draggedFieldIndex.value = index;
    }
    function enterFieldDrag(index) {
        if (drag.draggedFieldIndex.value !== null && drag.draggedFieldIndex.value !== index) {
            drag.dragOverFieldIndex.value = index;
        }
    }
    function dropField(target, targetIndex) {
        const sourceIndex = drag.draggedFieldIndex.value;
        if (sourceIndex !== null && sourceIndex !== targetIndex) {
            const [moved] = target.splice(sourceIndex, 1);
            target.splice(targetIndex, 0, moved);
        }
        endFieldDrag();
    }
    function endFieldDrag() {
        drag.draggedFieldIndex.value = null;
        drag.dragOverFieldIndex.value = null;
    }
    return { startFieldDrag, enterFieldDrag, dropField, endFieldDrag };
}

function createDatasetActions(ctx) {
    async function loadDatasets() {
        ctx.datasetLoading.value = true;
        try {
            const page = await datasetApi.listDatasets({
                page: ctx.datasetPage.value,
                size: ctx.datasetSize.value,
                keyword: ctx.datasetKeyword.value,
                ...sortParams(ctx.sortBy, ctx.sortOrder)
            });
            ctx.datasets.value = page.records;
            ctx.datasetTotal.value = page.total;
        }
        finally {
            ctx.datasetLoading.value = false;
        }
    }
    function openDataset(dataset) {
        ctx.router.push({ name: 'dataset-detail', params: { datasetId: dataset.id } });
    }
    function openCreateDialog() {
        ctx.createForm.name = '';
        ctx.createForm.description = '';
        ctx.createForm.fields = defaultFields();
        ctx.createVisible.value = true;
    }
    async function searchDatasets() {
        ctx.datasetPage.value = 1;
        await loadDatasets();
    }
    async function changeDatasetSize() {
        ctx.datasetPage.value = 1;
        await loadDatasets();
    }
    function toggleSort(field) {
        toggleDescSort(ctx.sortBy, ctx.sortOrder, field);
        loadDatasets();
    }
    async function submitCreate() {
        if (!validateCreateForm(ctx.createForm)) {
            return;
        } else {
            await runExclusive(ctx.creating, async () => {
                const name = ctx.createForm.name.trim();
                const page = await datasetApi.listDatasets({ page: 1, size: 100, keyword: name });
                if (page.records.some((dataset) => dataset.name === name)) {
                    ElMessage.warning('当前空间已存在同名评测集');
                } else {
                    const created = await datasetApi.createDataset({
                        name,
                        description: ctx.createForm.description,
                        fields: ctx.createForm.fields
                    });
                    ctx.createVisible.value = false;
                    ElMessage.success('评测集已创建');
                    await ctx.router.push({ name: 'dataset-detail', params: { datasetId: created.id } });
                }
            }).catch((error) => {
                ElMessage.error(getErrorMessage(error, '创建评测集失败'));
            });
        }
    }
    async function removeDataset(row) {
        await runExclusiveById(ctx.deletingIds, row.id, async () => {
            await ElMessageBox.confirm(`确定删除评测集“${row.name}”吗？`, '删除评测集', { type: 'warning' });
            await datasetApi.deleteDataset(row.id);
            ElMessage.success('已删除');
            movePreviousPageIfLastRow(ctx.datasets, ctx.datasetPage);
            await loadDatasets();
        });
    }
    function isDeletingDataset(datasetId) {
        return ctx.deletingIds.value.includes(datasetId);
    }
    return { loadDatasets, searchDatasets, changeDatasetSize, toggleSort, openDataset, openCreateDialog, submitCreate, removeDataset, isDeletingDataset };
}

function validateCreateForm(createForm) {
    if (!createForm.name.trim()) {
        ElMessage.warning('请输入评测集名称');
        return false;
    }
    if (!createForm.fields.length || createForm.fields.some((field) => !field.fieldName.trim())) {
        ElMessage.warning('请完善表结构');
        return false;
    }
    return true;
}

export function useDatasetList() {
    const router = useRouter();
    const datasetLoading = ref(false);
    const datasets = ref([]);
    const datasetTotal = ref(0);
    const datasetPage = ref(1);
    const datasetSize = ref(10);
    const datasetKeyword = ref('');
    const sortBy = ref('');
    const sortOrder = ref('');
    const createVisible = ref(false);
    const creating = ref(false);
    const deletingIds = ref([]);
    const createForm = reactive({ name: '', description: '', fields: defaultFields() });
    const drag = createFieldDragState();
    const actions = createDatasetActions({ router, datasetLoading, datasets, datasetTotal, datasetPage, datasetSize, datasetKeyword, sortBy, sortOrder, createVisible, creating, deletingIds, createForm });
    const descriptionEdit = useResourceDescriptionEdit({
        getId: (row) => row.id,
        getName: (row) => row.name,
        getDescription: (row) => row.description,
        update: datasetApi.updateDescription,
        reload: actions.loadDatasets
    });
    onMounted(async () => {
        await actions.loadDatasets();
    });

    return {
        datasetLoading,
        datasets,
        datasetTotal,
        datasetPage,
        datasetSize,
        datasetKeyword,
        sortBy,
        sortOrder,
        createVisible,
        creating,
        deletingIds,
        draggedFieldIndex: drag.draggedFieldIndex,
        dragOverFieldIndex: drag.dragOverFieldIndex,
        createForm,
        ...descriptionEdit,
        ...actions,
        addField,
        removeField,
        ...createDragActions(drag),
        formatTime: formatDateTime
    };
}
