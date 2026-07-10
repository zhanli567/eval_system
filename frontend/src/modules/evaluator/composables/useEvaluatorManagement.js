import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { evaluatorApi } from '../../../api/evaluator';
import { movePreviousPageIfLastRow, toggleDescSort } from '../../../utils/composableHelpers';
import { formatDateTime } from '../../../utils/formatters';
import { useColumnWidths } from '../../../utils/tableColumns';

function evaluatorColumns() {
    return useColumnWidths({
        evaluatorName: { width: 220, min: 160, max: 380 },
        evaluatorType: { width: 130, min: 100, max: 170 },
        latestVersionName: { width: 130, min: 110, max: 180 },
        description: { width: 280, min: 180, max: 520 },
        createdByName: { width: 140, min: 100, max: 220 },
        createdDate: { width: 190, min: 160, max: 240 },
        lastUpdatedByName: { width: 140, min: 100, max: 220 },
        lastUpdatedDate: { width: 190, min: 160, max: 240 },
        actions: { width: 140, min: 120, max: 180 }
    });
}

function presetParams(page, size, categoryId, keyword) {
    return { page: page.value, size: size.value, categoryId: categoryId.value, keyword: keyword.value };
}

function createEvaluatorActions(ctx) {
    return {
        ...createCategoryActions(ctx),
        ...createCustomActions(ctx),
        ...createPresetActions(ctx),
        ...createPickerActions(ctx),
        ...createEvaluatorNavigationActions(ctx)
    };
}

function createCategoryActions(ctx) {
    async function loadCategories() {
        ctx.categories.value = await evaluatorApi.listPresetCategories();
    }
    return { loadCategories };
}

function createCustomActions(ctx) {
    async function loadCustomEvaluators() {
        ctx.customLoading.value = true;
        try {
            const page = await evaluatorApi.listEvaluators({
                page: ctx.customPage.value,
                size: ctx.customSize.value,
                evaluatorType: ctx.customType.value,
                keyword: ctx.customKeyword.value,
                sortBy: ctx.customSortBy.value,
                sortOrder: ctx.customSortOrder.value
            });
            ctx.customEvaluators.value = page.records;
            ctx.customTotal.value = page.total;
        }
        finally {
            ctx.customLoading.value = false;
        }
    }
    async function searchCustom() {
        ctx.customPage.value = 1;
        await loadCustomEvaluators();
    }
    async function changeCustomSize() {
        ctx.customPage.value = 1;
        await loadCustomEvaluators();
    }
    async function toggleCustomSort(field) {
        toggleDescSort(ctx.customSortBy, ctx.customSortOrder, field);
        ctx.customPage.value = 1;
        await loadCustomEvaluators();
    }
    return { loadCustomEvaluators, searchCustom, changeCustomSize, toggleCustomSort };
}

function createPresetActions(ctx) {
    async function loadPresetEvaluators() {
        ctx.presetLoading.value = true;
        try {
            const page = await evaluatorApi.listPresetEvaluators(presetParams(ctx.presetPage, ctx.presetSize, ctx.activeCategoryId, ctx.presetKeyword));
            ctx.presetEvaluators.value = page.records;
            ctx.presetTotal.value = page.total;
        }
        finally {
            ctx.presetLoading.value = false;
        }
    }
    async function searchPreset() {
        ctx.presetPage.value = 1;
        await loadPresetEvaluators();
    }
    async function changePresetSize() {
        ctx.presetPage.value = 1;
        await loadPresetEvaluators();
    }
    async function selectPresetCategory(categoryId) {
        ctx.activeCategoryId.value = categoryId;
        ctx.presetPage.value = 1;
        await loadPresetEvaluators();
    }
    return { loadPresetEvaluators, searchPreset, changePresetSize, selectPresetCategory };
}

function createPickerActions(ctx) {
    async function openPicker() {
        ctx.pickerCategoryId.value = '';
        ctx.pickerKeyword.value = '';
        ctx.pickerPage.value = 1;
        ctx.pickerVisible.value = true;
        await loadPickerPresets();
    }
    async function loadPickerPresets() {
        ctx.pickerLoading.value = true;
        try {
            const page = await evaluatorApi.listPresetEvaluators(presetParams(ctx.pickerPage, ctx.pickerSize, ctx.pickerCategoryId, ctx.pickerKeyword));
            ctx.pickerPresets.value = page.records;
            ctx.pickerTotal.value = page.total;
        }
        finally {
            ctx.pickerLoading.value = false;
        }
    }
    async function searchPicker() {
        ctx.pickerPage.value = 1;
        await loadPickerPresets();
    }
    async function changePickerSize() {
        ctx.pickerPage.value = 1;
        await loadPickerPresets();
    }
    async function selectPickerCategory(categoryId) {
        ctx.pickerCategoryId.value = categoryId;
        ctx.pickerPage.value = 1;
        await loadPickerPresets();
    }
    return { openPicker, loadPickerPresets, searchPicker, changePickerSize, selectPickerCategory };
}

function createEvaluatorNavigationActions(ctx) {
    async function viewPreset(presetId) {
        ctx.detailVisible.value = true;
        ctx.detailLoading.value = true;
        try {
            ctx.selectedPreset.value = await evaluatorApi.getPresetEvaluator(presetId);
        }
        finally {
            ctx.detailLoading.value = false;
        }
    }
    function createCustom() {
        ctx.pickerVisible.value = false;
        ctx.router.push({ name: 'evaluator-create' });
    }
    function createFromPreset(presetId) {
        const preset = findPreset(ctx, presetId);
        if (preset?.evaluatorType === 'code') {
            ElMessage.warning('暂不支持Code型评估器');
            return;
        }
        ctx.pickerVisible.value = false;
        ctx.detailVisible.value = false;
        ctx.router.push({ name: 'evaluator-create', query: { presetId } });
    }
    function editEvaluator(row) {
        ctx.router.push({ name: 'evaluator-edit', params: { evaluatorId: row.id } });
    }
    async function removeEvaluator(row) {
        await ElMessageBox.confirm(`确定删除评估器“${row.evaluatorName}”吗？`, '删除评估器', { type: 'warning' });
        await evaluatorApi.deleteEvaluator(row.id);
        ElMessage.success('已删除');
        movePreviousPageIfLastRow(ctx.customEvaluators, ctx.customPage);
        await loadCustomEvaluators();
    }
    async function loadCustomEvaluators() {
        const customActions = createCustomActions(ctx);
        await customActions.loadCustomEvaluators();
    }
    return { viewPreset, createCustom, createFromPreset, editEvaluator, removeEvaluator };
}

function findPreset(ctx, presetId) {
    return [...ctx.presetEvaluators.value, ...ctx.pickerPresets.value, ctx.selectedPreset.value]
        .filter(Boolean)
        .find((item) => item.id === presetId);
}

function typeLabel(type) {
    return type === 'code' ? 'Code' : 'LLM';
}

export function useEvaluatorManagement() {
    const router = useRouter();
    const activeTab = ref('custom');
    const customLoading = ref(false);
    const customEvaluators = ref([]);
    const customTotal = ref(0);
    const customPage = ref(1);
    const customSize = ref(10);
    const customKeyword = ref('');
    const customType = ref('');
    const customSortBy = ref('lastUpdatedDate');
    const customSortOrder = ref('desc');
    const categories = ref([]);
    const activeCategoryId = ref('');
    const presetLoading = ref(false);
    const presetEvaluators = ref([]);
    const presetTotal = ref(0);
    const presetPage = ref(1);
    const presetSize = ref(10);
    const presetKeyword = ref('');
    const pickerVisible = ref(false);
    const pickerCategoryId = ref('');
    const pickerKeyword = ref('');
    const pickerPage = ref(1);
    const pickerSize = ref(10);
    const pickerTotal = ref(0);
    const pickerLoading = ref(false);
    const pickerPresets = ref([]);
    const detailVisible = ref(false);
    const detailLoading = ref(false);
    const selectedPreset = ref(null);
    const columns = evaluatorColumns();
    const categoryOptions = computed(() => [
        { id: '', categoryName: '全部分类', displayOrder: 0 },
        ...categories.value
    ]);
    const ctx = { router, customLoading, customEvaluators, customTotal, customPage, customSize, customKeyword, customType, customSortBy, customSortOrder, categories, activeCategoryId, presetLoading, presetEvaluators, presetTotal, presetPage, presetSize, presetKeyword, pickerVisible, pickerCategoryId, pickerKeyword, pickerPage, pickerSize, pickerTotal, pickerLoading, pickerPresets, detailVisible, detailLoading, selectedPreset };
    const actions = createEvaluatorActions(ctx);
    onMounted(async () => {
        await Promise.all([actions.loadCategories(), actions.loadCustomEvaluators()]);
        await actions.loadPresetEvaluators();
    });

    return {
        activeTab,
        customLoading,
        customEvaluators,
        customTotal,
        customPage,
        customSize,
        customKeyword,
        customType,
        customSortBy,
        customSortOrder,
        categories,
        columnWidths: columns.columnWidths,
        categoryOptions,
        activeCategoryId,
        presetLoading,
        presetEvaluators,
        presetTotal,
        presetPage,
        presetSize,
        presetKeyword,
        pickerVisible,
        pickerCategoryId,
        pickerKeyword,
        pickerPage,
        pickerSize,
        pickerTotal,
        pickerLoading,
        pickerPresets,
        detailVisible,
        detailLoading,
        selectedPreset,
        ...actions,
        handleColumnResize: columns.handleColumnResize,
        typeLabel,
        formatTime: formatDateTime
    };
}
