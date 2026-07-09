import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { tagApi } from '../../../api/tag';
import { formatDateTime } from '../../../utils/formatters';
import { useColumnWidths } from '../../../utils/tableColumns';
export const tagTypeOptions = [
    { label: '分类', value: 'category' },
    { label: '布尔值', value: 'boolean' },
    { label: '数字', value: 'number' },
    { label: '文本', value: 'text' }
];
const booleanOptions = [
    { optionName: 'True', optionGroup: 'pass' },
    { optionName: 'False', optionGroup: 'fail' }
];
export function useTagManagement() {
    const tagLoading = ref(false);
    const saving = ref(false);
    const tags = ref([]);
    const tagTotal = ref(0);
    const tagPage = ref(1);
    const tagSize = ref(10);
    const tagKeyword = ref('');
    const tagType = ref('');
    const sortBy = ref('lastUpdatedDate');
    const sortOrder = ref('desc');
    const dialogVisible = ref(false);
    const detailDialogVisible = ref(false);
    const detailLoading = ref(false);
    const tagDetail = ref(null);
    const editingId = ref('');
    const tagForm = reactive({
        tagName: '',
        description: '',
        tagType: 'category',
        minValue: undefined,
        maxValue: undefined,
        passThreshold: undefined,
        passOptions: [''],
        failOptions: ['']
    });
    const editing = computed(() => Boolean(editingId.value));
    const dialogTitle = computed(() => (editing.value ? '编辑标签' : '创建标签'));
    const detailPassOptions = computed(() => tagDetail.value?.options.filter((option) => option.optionGroup === 'pass') ?? []);
    const detailFailOptions = computed(() => tagDetail.value?.options.filter((option) => option.optionGroup === 'fail') ?? []);
    const { columnWidths, handleColumnResize } = useColumnWidths({
        tagName: { width: 220, min: 160, max: 380 },
        tagType: { width: 140, min: 110, max: 180 },
        description: { width: 280, min: 180, max: 520 },
        createdByName: { width: 140, min: 100, max: 220 },
        createdDate: { width: 190, min: 160, max: 240 },
        lastUpdatedByName: { width: 140, min: 100, max: 220 },
        lastUpdatedDate: { width: 190, min: 160, max: 240 },
        actions: { width: 180, min: 150, max: 220 }
    });
    onMounted(async () => {
        await loadTags();
    });
    async function loadTags() {
        tagLoading.value = true;
        try {
            const page = await tagApi.listTags({
                page: tagPage.value,
                size: tagSize.value,
                tagType: tagType.value,
                keyword: tagKeyword.value,
                sortBy: sortBy.value,
                sortOrder: sortOrder.value
            });
            tags.value = page.records;
            tagTotal.value = page.total;
        }
        finally {
            tagLoading.value = false;
        }
    }
    function openCreateDialog() {
        editingId.value = '';
        resetForm();
        dialogVisible.value = true;
    }
    async function openEditDialog(row) {
        const detail = await tagApi.getTag(row.id);
        editingId.value = row.id;
        fillForm(detail);
        dialogVisible.value = true;
    }
    async function openDetailDialog(row) {
        detailDialogVisible.value = true;
        detailLoading.value = true;
        tagDetail.value = null;
        try {
            tagDetail.value = await tagApi.getTag(row.id);
        }
        catch (error) {
            detailDialogVisible.value = false;
            ElMessage.error(getErrorMessage(error, '加载标签详情失败'));
        }
        finally {
            detailLoading.value = false;
        }
    }
    async function submitTag() {
        try {
            validateForm();
            saving.value = true;
            const payload = buildPayload();
            if (editingId.value) {
                await tagApi.updateTag(editingId.value, payload);
                ElMessage.success('标签已更新');
            }
            else {
                await tagApi.createTag(payload);
                ElMessage.success('标签已创建');
            }
            dialogVisible.value = false;
            await loadTags();
        }
        catch (error) {
            ElMessage.error(getErrorMessage(error, '保存失败'));
        }
        finally {
            saving.value = false;
        }
    }
    function searchTags() {
        tagPage.value = 1;
        return loadTags();
    }
    function changeTagSize() {
        tagPage.value = 1;
        return loadTags();
    }
    function toggleSort(field) {
        if (sortBy.value === field) {
            sortOrder.value = sortOrder.value === 'desc' ? 'asc' : 'desc';
        }
        else {
            sortBy.value = field;
            sortOrder.value = 'desc';
        }
        tagPage.value = 1;
        return loadTags();
    }
    async function removeTag(row) {
        await ElMessageBox.confirm(`确定删除标签“${row.tagName}”吗？`, '删除标签', { type: 'warning' });
        await tagApi.deleteTag(row.id);
        ElMessage.success('已删除');
        if (tags.value.length === 1 && tagPage.value > 1) {
            tagPage.value -= 1;
        }
        await loadTags();
    }
    function resetForm() {
        tagForm.tagName = '';
        tagForm.description = '';
        tagForm.tagType = 'category';
        tagForm.minValue = undefined;
        tagForm.maxValue = undefined;
        tagForm.passThreshold = undefined;
        tagForm.passOptions = [''];
        tagForm.failOptions = [''];
    }
    function fillForm(detail) {
        tagForm.tagName = detail.tagName;
        tagForm.description = detail.description || '';
        tagForm.tagType = detail.tagType;
        tagForm.minValue = detail.minValue;
        tagForm.maxValue = detail.maxValue;
        tagForm.passThreshold = detail.passThreshold;
        const passOptions = detail.options.filter((option) => option.optionGroup === 'pass').map((option) => option.optionName);
        const failOptions = detail.options.filter((option) => option.optionGroup === 'fail').map((option) => option.optionName);
        tagForm.passOptions = passOptions.length ? passOptions : [''];
        tagForm.failOptions = failOptions.length ? failOptions : [''];
    }
    function addCategoryOption(group) {
        const target = group === 'pass' ? tagForm.passOptions : tagForm.failOptions;
        if (target.length >= 5) {
            ElMessage.warning('Pass和Fail选项每组最多支持5个');
            return;
        }
        target.push('');
    }
    function removeCategoryOption(group, index) {
        const target = group === 'pass' ? tagForm.passOptions : tagForm.failOptions;
        if (target.length === 1) {
            target[0] = '';
            return;
        }
        target.splice(index, 1);
    }
    function validateForm() {
        if (!tagForm.tagName.trim()) {
            throw new Error('请输入标签名称');
        }
        if (tagForm.tagName.trim().length > 50) {
            throw new Error('标签名称不能超过50个字符');
        }
        if (tagForm.description.trim().length > 200) {
            throw new Error('描述不能超过200个字符');
        }
        if (tagForm.tagType === 'category') {
            if (!cleanOptions(tagForm.passOptions).length || !cleanOptions(tagForm.failOptions).length) {
                throw new Error('分类标签请至少配置一个Pass选项和一个Fail选项');
            }
        }
        if (tagForm.tagType === 'number') {
            if (!tagForm.minValue || !tagForm.maxValue || !tagForm.passThreshold) {
                throw new Error('请维护评分范围和通过阈值');
            }
            if (tagForm.minValue >= tagForm.maxValue) {
                throw new Error('评分最大值必须大于最小值');
            }
            if (tagForm.passThreshold < tagForm.minValue || tagForm.passThreshold > tagForm.maxValue) {
                throw new Error('通过阈值必须介于评分范围内');
            }
        }
    }
    function buildPayload() {
        const options = tagForm.tagType === 'category'
            ? [
                ...cleanOptions(tagForm.passOptions).map((optionName) => ({ optionName, optionGroup: 'pass' })),
                ...cleanOptions(tagForm.failOptions).map((optionName) => ({ optionName, optionGroup: 'fail' }))
            ]
            : [];
        return {
            tagName: tagForm.tagName.trim(),
            description: tagForm.description.trim(),
            tagType: tagForm.tagType,
            minValue: tagForm.tagType === 'number' ? tagForm.minValue : undefined,
            maxValue: tagForm.tagType === 'number' ? tagForm.maxValue : undefined,
            passThreshold: tagForm.tagType === 'number' ? tagForm.passThreshold : undefined,
            options
        };
    }
    function cleanOptions(options) {
        return options.map((item) => item.trim()).filter(Boolean);
    }
    function getTagTypeLabel(value) {
        return tagTypeOptions.find((item) => item.value === value)?.label ?? '-';
    }
    const formatTime = formatDateTime;
    function getErrorMessage(error, fallback) {
        if (error instanceof Error) {
            return error.message || fallback;
        }
        const axiosError = error;
        return axiosError.response?.data?.msg || axiosError.response?.data?.message || fallback;
    }
    return {
        tagLoading,
        saving,
        tags,
        tagTotal,
        tagPage,
        tagSize,
        tagKeyword,
        tagType,
        sortBy,
        sortOrder,
        dialogVisible,
        detailDialogVisible,
        detailLoading,
        tagDetail,
        detailPassOptions,
        detailFailOptions,
        editing,
        dialogTitle,
        tagForm,
        columnWidths,
        tagTypeOptions,
        booleanOptions,
        loadTags,
        searchTags,
        changeTagSize,
        toggleSort,
        openCreateDialog,
        openEditDialog,
        openDetailDialog,
        submitTag,
        removeTag,
        addCategoryOption,
        removeCategoryOption,
        handleColumnResize,
        getTagTypeLabel,
        formatTime
    };
}
