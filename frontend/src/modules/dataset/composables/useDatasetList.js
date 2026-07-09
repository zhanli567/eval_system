import { onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { datasetApi } from '../../../api/dataset';
import { formatDateTime } from '../../../utils/formatters';
import { useColumnWidths } from '../../../utils/tableColumns';
function defaultFields() {
    return [
        { fieldName: 'query', fieldType: 'string', required: true, description: '用户问题' },
        { fieldName: 'reference_response', fieldType: 'string', required: false, description: '参考答案' }
    ];
}
export function useDatasetList() {
    const router = useRouter();
    const datasetLoading = ref(false);
    const datasets = ref([]);
    const datasetTotal = ref(0);
    const datasetPage = ref(1);
    const datasetSize = ref(10);
    const datasetKeyword = ref('');
    const sortBy = ref('lastUpdatedDate');
    const sortOrder = ref('desc');
    const createVisible = ref(false);
    const draggedFieldIndex = ref(null);
    const dragOverFieldIndex = ref(null);
    const createForm = reactive({
        name: '',
        description: '',
        fields: defaultFields()
    });
    const { columnWidths, handleColumnResize } = useColumnWidths({
        name: { width: 240, min: 180, max: 420 },
        publishedVersionCount: { width: 120, min: 100, max: 180 },
        latestItemCount: { width: 110, min: 90, max: 170 },
        description: { width: 280, min: 180, max: 520 },
        createdByName: { width: 140, min: 100, max: 220 },
        createdDate: { width: 190, min: 160, max: 240 },
        lastUpdatedByName: { width: 140, min: 100, max: 220 },
        lastUpdatedDate: { width: 190, min: 160, max: 240 },
        actions: { width: 140, min: 120, max: 180 }
    });
    onMounted(async () => {
        await loadDatasets();
    });
    async function loadDatasets() {
        datasetLoading.value = true;
        try {
            const page = await datasetApi.listDatasets({
                page: datasetPage.value,
                size: datasetSize.value,
                keyword: datasetKeyword.value,
                sortBy: sortBy.value,
                sortOrder: sortOrder.value
            });
            datasets.value = page.records;
            datasetTotal.value = page.total;
        }
        finally {
            datasetLoading.value = false;
        }
    }
    function openDataset(dataset) {
        router.push({ name: 'dataset-detail', params: { datasetId: dataset.id } });
    }
    function openCreateDialog() {
        createForm.name = '';
        createForm.description = '';
        createForm.fields = defaultFields();
        createVisible.value = true;
    }
    async function searchDatasets() {
        datasetPage.value = 1;
        await loadDatasets();
    }
    async function changeDatasetSize() {
        datasetPage.value = 1;
        await loadDatasets();
    }
    function toggleSort(field) {
        if (sortBy.value === field) {
            sortOrder.value = sortOrder.value === 'desc' ? 'asc' : 'desc';
        }
        else {
            sortBy.value = field;
            sortOrder.value = 'desc';
        }
        loadDatasets();
    }
    async function submitCreate() {
        if (!createForm.name.trim()) {
            ElMessage.warning('请输入评测集名称');
            return;
        }
        if (!createForm.fields.length || createForm.fields.some((field) => !field.fieldName.trim())) {
            ElMessage.warning('请完善表结构');
            return;
        }
        const created = await datasetApi.createDataset({
            name: createForm.name,
            description: createForm.description,
            fields: createForm.fields
        });
        createVisible.value = false;
        ElMessage.success('评测集已创建');
        await router.push({ name: 'dataset-detail', params: { datasetId: created.id } });
    }
    async function removeDataset(row) {
        await ElMessageBox.confirm(`确定删除评测集“${row.name}”吗？`, '删除评测集', { type: 'warning' });
        await datasetApi.deleteDataset(row.id);
        ElMessage.success('已删除');
        if (datasets.value.length === 1 && datasetPage.value > 1) {
            datasetPage.value -= 1;
        }
        await loadDatasets();
    }
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
        draggedFieldIndex.value = index;
    }
    function enterFieldDrag(index) {
        if (draggedFieldIndex.value !== null && draggedFieldIndex.value !== index) {
            dragOverFieldIndex.value = index;
        }
    }
    function dropField(target, targetIndex) {
        const sourceIndex = draggedFieldIndex.value;
        if (sourceIndex === null || sourceIndex === targetIndex) {
            draggedFieldIndex.value = null;
            dragOverFieldIndex.value = null;
            return;
        }
        const [moved] = target.splice(sourceIndex, 1);
        target.splice(targetIndex, 0, moved);
        draggedFieldIndex.value = null;
        dragOverFieldIndex.value = null;
    }
    function endFieldDrag() {
        draggedFieldIndex.value = null;
        dragOverFieldIndex.value = null;
    }
    const formatTime = formatDateTime;
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
        draggedFieldIndex,
        dragOverFieldIndex,
        createForm,
        columnWidths,
        loadDatasets,
        searchDatasets,
        changeDatasetSize,
        toggleSort,
        openDataset,
        openCreateDialog,
        submitCreate,
        removeDataset,
        addField,
        removeField,
        startFieldDrag,
        enterFieldDrag,
        dropField,
        endFieldDrag,
        handleColumnResize,
        formatTime
    };
}
