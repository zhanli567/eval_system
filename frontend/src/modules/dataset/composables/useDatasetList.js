import { onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { datasetApi } from '../../../api/dataset';
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
    const datasetSize = ref(8);
    const datasetKeyword = ref('');
    const createVisible = ref(false);
    const draggedFieldIndex = ref(null);
    const dragOverFieldIndex = ref(null);
    const createForm = reactive({
        name: '',
        description: '',
        fields: defaultFields()
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
                keyword: datasetKeyword.value
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
        await loadDatasets();
    }
    function addField(target) {
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
    function formatTime(value) {
        if (!value)
            return '-';
        const numberValue = Number(value);
        if (Number.isNaN(numberValue))
            return value;
        return new Date(numberValue).toLocaleString();
    }
    return {
        datasetLoading,
        datasets,
        datasetTotal,
        datasetPage,
        datasetSize,
        datasetKeyword,
        createVisible,
        draggedFieldIndex,
        dragOverFieldIndex,
        createForm,
        loadDatasets,
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
        formatTime
    };
}
