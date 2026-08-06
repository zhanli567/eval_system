import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { tagApi } from '../../../api/tag';
import { getErrorMessage, movePreviousPageIfLastRow, toggleDescSort } from '../../../utils/composableHelpers';
import { formatDateTime } from '../../../utils/formatters';

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

function resetForm(form) {
    Object.assign(form, {
        tagName: '',
        description: '',
        tagType: 'category',
        minValue: undefined,
        maxValue: undefined,
        passThreshold: undefined,
        passOptions: [''],
        failOptions: ['']
    });
}

function cleanOptions(options) {
    return options.map((item) => item.trim()).filter(Boolean);
}

function fillForm(form, detail) {
    form.tagName = detail.tagName;
    form.description = detail.description || '';
    form.tagType = detail.tagType;
    form.minValue = detail.minValue;
    form.maxValue = detail.maxValue;
    form.passThreshold = detail.passThreshold;
    form.passOptions = optionNames(detail, 'pass');
    form.failOptions = optionNames(detail, 'fail');
}

function optionNames(detail, group) {
    const names = detail.options.filter((option) => option.optionGroup === group).map((option) => option.optionName);
    return names.length ? names : [''];
}

function getFormError(form) {
    if (!form.tagName.trim()) {
        return '请输入标签名称';
    } else if (form.tagName.trim().length > 50) {
        return '标签名称不能超过50个字符';
    } else if (form.description.trim().length > 200) {
        return '描述不能超过200个字符';
    } else if (form.tagType === 'category') {
        return getCategoryOptionsError(form);
    } else if (form.tagType === 'number') {
        return getNumberRangeError(form);
    } else {
        return '';
    }
}

function getCategoryOptionsError(form) {
    if (!cleanOptions(form.passOptions).length || !cleanOptions(form.failOptions).length) {
        return '分类标签请至少配置一个Pass选项和一个Fail选项';
    } else {
        return '';
    }
}

function getNumberRangeError(form) {
    if (!form.minValue || !form.maxValue || !form.passThreshold) {
        return '请维护评分范围和通过阈值';
    } else if (form.minValue >= form.maxValue) {
        return '评分最大值必须大于最小值';
    } else if (form.passThreshold < form.minValue || form.passThreshold > form.maxValue) {
        return '通过阈值必须介于评分范围内';
    } else {
        return '';
    }
}

function buildPayload(form) {
    return {
        tagName: form.tagName.trim(),
        description: form.description.trim(),
        tagType: form.tagType,
        minValue: form.tagType === 'number' ? form.minValue : undefined,
        maxValue: form.tagType === 'number' ? form.maxValue : undefined,
        passThreshold: form.tagType === 'number' ? form.passThreshold : undefined,
        options: form.tagType === 'category' ? buildCategoryOptions(form) : []
    };
}

function buildCategoryOptions(form) {
    return [
        ...cleanOptions(form.passOptions).map((optionName) => ({ optionName, optionGroup: 'pass' })),
        ...cleanOptions(form.failOptions).map((optionName) => ({ optionName, optionGroup: 'fail' }))
    ];
}

function createTagActions(ctx) {
    async function loadTags() {
        ctx.tagLoading.value = true;
        try {
            const page = await tagApi.listTags({
                page: ctx.tagPage.value,
                size: ctx.tagSize.value,
                tagType: ctx.tagType.value,
                keyword: ctx.tagKeyword.value,
                sortBy: ctx.sortBy.value,
                sortOrder: ctx.sortOrder.value
            });
            ctx.tags.value = page.records;
            ctx.tagTotal.value = page.total;
        }
        finally {
            ctx.tagLoading.value = false;
        }
    }
    return {
        loadTags,
        ...createTagDialogActions(ctx),
        ...createTagSubmitActions(ctx, loadTags),
        ...createTagListActions(ctx, loadTags),
        ...createTagOptionActions(ctx)
    };
}

function createTagDialogActions(ctx) {
    function openCreateDialog() {
        ctx.editingId.value = '';
        resetForm(ctx.tagForm);
        ctx.dialogVisible.value = true;
    }
    async function openEditDialog(row) {
        const detail = await tagApi.getTag(row.id);
        ctx.editingId.value = row.id;
        fillForm(ctx.tagForm, detail);
        ctx.dialogVisible.value = true;
    }
    async function openDetailDialog(row) {
        ctx.detailDialogVisible.value = true;
        ctx.detailLoading.value = true;
        ctx.tagDetail.value = null;
        try {
            ctx.tagDetail.value = await tagApi.getTag(row.id);
        }
        catch (error) {
            ctx.detailDialogVisible.value = false;
            ElMessage.error(getErrorMessage(error, '加载标签详情失败'));
        }
        finally {
            ctx.detailLoading.value = false;
        }
    }
    return { openCreateDialog, openEditDialog, openDetailDialog };
}

function createTagSubmitActions(ctx, loadTags) {
    async function submitTag() {
        const errorMessage = getFormError(ctx.tagForm);
        if (errorMessage) {
            ElMessage.error(errorMessage);
            return;
        } else {
            ctx.saving.value = true;
        }
        try {
            const name = ctx.tagForm.tagName.trim();
            const page = ctx.editingId.value
                ? null
                : await tagApi.listTags({ page: 1, size: 100, keyword: name });
            if (page?.records.some((tag) => tag.tagName === name)) {
                ElMessage.error('当前空间已存在同名标签');
            } else {
                await saveTag();
                ctx.dialogVisible.value = false;
                await loadTags();
            }
        }
        finally {
            ctx.saving.value = false;
        }
    }
    async function saveTag() {
        if (ctx.editingId.value) {
            await tagApi.updateTag(ctx.editingId.value, buildPayload(ctx.tagForm));
            ElMessage.success('标签已更新');
            return;
        }
        await tagApi.createTag(buildPayload(ctx.tagForm));
        ElMessage.success('标签已创建');
    }
    return { submitTag };
}

function createTagListActions(ctx, loadTags) {
    function searchTags() {
        ctx.tagPage.value = 1;
        return loadTags();
    }
    function changeTagSize() {
        ctx.tagPage.value = 1;
        return loadTags();
    }
    function toggleSort(field) {
        toggleDescSort(ctx.sortBy, ctx.sortOrder, field);
        ctx.tagPage.value = 1;
        return loadTags();
    }
    async function removeTag(row) {
        await ElMessageBox.confirm(`确定删除标签“${row.tagName}”吗？`, '删除标签', { type: 'warning' });
        await tagApi.deleteTag(row.id);
        ElMessage.success('已删除');
        movePreviousPageIfLastRow(ctx.tags, ctx.tagPage);
        await loadTags();
    }
    return { searchTags, changeTagSize, toggleSort, removeTag };
}

function createTagOptionActions(ctx) {
    function addCategoryOption(group) {
        const target = group === 'pass' ? ctx.tagForm.passOptions : ctx.tagForm.failOptions;
        if (target.length >= 5) {
            ElMessage.warning('Pass和Fail选项每组最多支持5个');
            return;
        }
        target.push('');
    }
    function removeCategoryOption(group, index) {
        const target = group === 'pass' ? ctx.tagForm.passOptions : ctx.tagForm.failOptions;
        target.length === 1 ? target[0] = '' : target.splice(index, 1);
    }
    return { addCategoryOption, removeCategoryOption };
}

function getTagTypeLabel(value) {
    return tagTypeOptions.find((item) => item.value === value)?.label ?? '-';
}

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
    const tagForm = reactive({});
    resetForm(tagForm);
    const editing = computed(() => Boolean(editingId.value));
    const dialogTitle = computed(() => (editing.value ? '编辑标签' : '创建标签'));
    const detailPassOptions = computed(() => tagDetail.value?.options.filter((option) => option.optionGroup === 'pass') ?? []);
    const detailFailOptions = computed(() => tagDetail.value?.options.filter((option) => option.optionGroup === 'fail') ?? []);
    const ctx = { tagLoading, saving, tags, tagTotal, tagPage, tagSize, tagKeyword, tagType, sortBy, sortOrder, dialogVisible, detailDialogVisible, detailLoading, tagDetail, editingId, tagForm };
    const actions = createTagActions(ctx);
    onMounted(async () => {
        await actions.loadTags();
    });

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
        tagTypeOptions,
        booleanOptions,
        ...actions,
        getTagTypeLabel,
        formatTime: formatDateTime
    };
}
