import { computed, ref } from 'vue';
import { taskApi } from '../../../api/task';

function metricDimensions(source) {
    return source.value?.dimensions ?? [];
}

async function loadTaskMetrics(ctx, options = {}) {
    if (!ctx.taskId.value) {
        return;
    }
    const silent = Boolean(options.silent);
    if (!silent) {
        ctx.metricsLoading.value = true;
    }
    try {
        const [overview, scoreSummary, itemDistribution] = await Promise.all([
            taskApi.getMetricOverview(ctx.taskId.value),
            taskApi.getMetricScoreSummary(ctx.taskId.value),
            taskApi.getMetricItemDistribution(ctx.taskId.value)
        ]);
        ctx.metricOverview.value = overview;
        ctx.metricScoreSummary.value = scoreSummary;
        ctx.metricItemDistribution.value = itemDistribution;
    } finally {
        if (!silent) {
            ctx.metricsLoading.value = false;
        }
    }
}

export function useTaskMetrics(taskId) {
    const activeTab = ref('data');
    const metricsLoading = ref(false);
    const metricOverview = ref();
    const metricScoreSummary = ref();
    const metricItemDistribution = ref();
    const ctx = { taskId, activeTab, metricsLoading, metricOverview, metricScoreSummary, metricItemDistribution };

    async function loadMetrics(options = {}) {
        await loadTaskMetrics(ctx, options);
    }

    async function changeTaskDetailTab(tab) {
        activeTab.value = tab;
        if (tab === 'metrics') {
            await loadMetrics();
        }
    }

    return {
        activeTab,
        metricsLoading,
        metricOverview,
        metricScoreSummary,
        metricItemDistribution,
        metricScoreDimensions: computed(() => metricDimensions(metricScoreSummary)),
        metricDistributionDimensions: computed(() => metricDimensions(metricItemDistribution)),
        loadMetrics,
        changeTaskDetailTab
    };
}
