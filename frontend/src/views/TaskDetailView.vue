<script setup>
import { computed, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { CircleCheck, CircleClose, Clock, Delete, Loading, Operation, PriceTag, Refresh } from '@element-plus/icons-vue';
import EChartView from '../components/EChartView.vue';
import TagCreateDialog from '../components/TagCreateDialog.vue';
import TaskTagDrawer from '../components/TaskTagDrawer.vue';
import { useTaskDetail } from '../modules/task/composables/useTaskDetail';
import { formatAgentOutputValue, formatAppOutput, formatEvaluatorReason } from '../utils/taskDisplay';
const route = useRoute();
const taskId = computed(() => String(route.params.taskId ?? ''));
const {
    loading,
    stopping,
    page,
    size,
    base,
    evaluators,
    tags,
    rows,
    total,
    canStopTask,
    tagDrawerVisible,
    tagKeyword,
    tagTypeFilter,
    tagPage,
    tagSize,
    tagTotal,
    tagLoading,
    tagOperatingIds,
    allTags,
    selectedTagIds,
    tagTypeOptions,
    columnSettingVisible,
    columnSettingItems,
    visibleTableColumns,
    activeTab,
    metricsLoading,
    metricOverview,
    metricScoreDimensions,
    metricDistributionDimensions,
    loadDetail,
    changeTaskDetailTab,
    loadAllTags,
    backToList,
    stopTask,
    openAnnotation,
    changeSize,
    openTagDrawer,
    addTaskTag,
    removeTaskTag,
    removeTaskTagByTag,
    searchAllTags,
    changeTagSize,
    setColumnVisible,
    resetColumnSettings,
    confirmColumnSettings,
    startColumnDrag,
    enterColumnDrag,
    finishColumnDrag,
    formatAppBinding,
    statusLabel,
    passTagType,
    tagTypeLabel,
    formatTime
} = useTaskDetail(taskId);
const tagCreateVisible = ref(false);
const statusIcons = {
    pending: Clock,
    running: Loading,
    completed: CircleCheck,
    failed: CircleClose,
    annotation_pending: Clock,
    annotating: Loading,
    skipped: Clock,
    stopped: CircleClose
};
const selectedDistributionId = ref('');
const progress = computed(() => metricOverview.value?.progress ?? {});
const hasMetricDimensions = computed(() => metricScoreDimensions.value.length > 0);
const selectedDistribution = computed(() => (
    metricDistributionDimensions.value.find((item) => item.dimensionId === selectedDistributionId.value)
    ?? metricDistributionDimensions.value[0]
));
const progressCards = computed(() => [
    { label: '评测进度', value: metricPercent(progress.value.progressRate) },
    { label: '评测集总量', value: metricNumber(progress.value.totalCount) },
    { label: '未完成量', value: metricNumber(progress.value.incompleteCount) },
    { label: '已完成量', value: metricNumber(progress.value.completedCount) }
]);
const gaugeOption = computed(() => ({
    color: ['#2f8cff'],
    series: [
        {
            type: 'gauge',
            startAngle: 210,
            endAngle: -30,
            min: 0,
            max: 100,
            radius: '92%',
            progress: {
                show: true,
                width: 16,
                roundCap: true
            },
            axisLine: {
                lineStyle: {
                    width: 16,
                    color: [[1, '#edf2fa']]
                }
            },
            pointer: { show: false },
            axisTick: { show: false },
            splitLine: { show: false },
            axisLabel: { show: false },
            anchor: { show: false },
            title: { show: false },
            detail: {
                offsetCenter: [0, '18%'],
                color: '#202642',
                fontSize: 32,
                fontWeight: 700,
                formatter: () => metricPercent(metricOverview.value?.overallScore)
            },
            data: [{ value: Number(metricOverview.value?.overallScore ?? 0) }]
        }
    ]
}));
const scoreChartOption = computed(() => ({
    color: ['#2f8cff'],
    grid: { left: 48, right: 24, top: 28, bottom: 52 },
    tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' },
        formatter: scoreTooltip
    },
    xAxis: {
        type: 'category',
        data: metricScoreDimensions.value.map(dimensionName),
        axisTick: { show: false },
        axisLabel: {
            color: '#8d94a6',
            interval: 0,
            overflow: 'truncate',
            width: 120
        }
    },
    yAxis: {
        type: 'value',
        max: 100,
        axisLabel: { formatter: '{value}%', color: '#8d94a6' },
        splitLine: { lineStyle: { color: '#edf1f7' } }
    },
    series: [
        {
            name: '通过率',
            type: 'bar',
            barWidth: 28,
            itemStyle: {
                borderRadius: [6, 6, 0, 0],
                color: (params) => (params.data.dimensionType === 'tag' ? '#45bf8f' : '#2f8cff')
            },
            data: metricScoreDimensions.value.map((item) => ({
                value: item.passRate ?? 0,
                dimensionType: item.dimensionType
            }))
        }
    ]
}));
const distributionStackOption = computed(() => ({
    color: ['#45bf8f', '#ff6b6b', '#a8afbf'],
    legend: {
        top: 0,
        right: 8,
        itemWidth: 10,
        itemHeight: 10,
        textStyle: { color: '#6b7280' }
    },
    grid: { left: 46, right: 24, top: 42, bottom: 48 },
    tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' },
        formatter: stackTooltip
    },
    xAxis: {
        type: 'category',
        data: metricDistributionDimensions.value.map(dimensionName),
        axisTick: { show: false },
        axisLabel: {
            color: '#8d94a6',
            interval: 0,
            overflow: 'truncate',
            width: 120
        }
    },
    yAxis: {
        type: 'value',
        axisLabel: { color: '#8d94a6' },
        splitLine: { lineStyle: { color: '#edf1f7' } }
    },
    series: [
        stackSeries('通过', 'passCount'),
        stackSeries('未通过', 'failedCount'),
        stackSeries('未完成', 'pendingCount')
    ]
}));
const distributionPieOption = computed(() => ({
    color: ['#45bf8f', '#ff6b6b', '#a8afbf'],
    tooltip: { trigger: 'item' },
    legend: {
        bottom: 0,
        left: 'center',
        itemWidth: 10,
        itemHeight: 10,
        textStyle: { color: '#6b7280' }
    },
    title: {
        text: dimensionName(selectedDistribution.value),
        subtext: '数据项分布',
        left: 'center',
        top: 16,
        textStyle: {
            color: '#202642',
            fontSize: 15,
            fontWeight: 700
        },
        subtextStyle: {
            color: '#8d94a6',
            fontSize: 12
        }
    },
    series: [
        {
            type: 'pie',
            radius: ['48%', '70%'],
            center: ['50%', '56%'],
            avoidLabelOverlap: true,
            label: { formatter: '{b}: {c}' },
            data: [
                { name: '通过', value: selectedDistribution.value?.passCount ?? 0 },
                { name: '未通过', value: selectedDistribution.value?.failedCount ?? 0 },
                { name: '未完成', value: selectedDistribution.value?.pendingCount ?? 0 }
            ]
        }
    ]
}));
watch(metricDistributionDimensions, (dimensions) => {
    const exists = dimensions.some((item) => item.dimensionId === selectedDistributionId.value);
    selectedDistributionId.value = exists ? selectedDistributionId.value : (dimensions[0]?.dimensionId ?? '');
}, { immediate: true });
function statusIcon(value) {
    return statusIcons[value] || Clock;
}
function statusIconClass(value) {
    return `is-${value || 'pending'}`;
}
function findTagResult(row, taskTagId) {
    return row.tagResults.find((item) => item.taskTagId === taskTagId);
}
function tagResultValue(row, taskTagId) {
    const result = findTagResult(row, taskTagId);
    if (!result) {
        return '-';
    }
    return result.optionName || result.valueText || (result.valueNumber ?? '-');
}
function findEvaluatorResult(row, taskEvaluatorId) {
    return row.evaluatorResults.find((item) => item.taskEvaluatorId === taskEvaluatorId);
}
function formatNameVersion(name, version) {
    return `${name || '-'} / ${version || '-'}`;
}
function formatPassRate(value) {
    return value === undefined || value === null ? '-' : `${value}%`;
}
function formatEvaluatorDimension(evaluator) {
    const name = evaluator.evaluatorName || evaluator.versionName || '-';
    return `${name} / ${evaluator.versionName || '-'} / 通过率 ${formatPassRate(evaluator.passRate)}`;
}
function formatTagDimension(tag) {
    return `${tag.tagName || '-'}（${tagTypeLabel(tag.tagType)}）`;
}
function isScoredEvaluatorResult(result) {
    return Boolean(result && (result.status === 'completed' || result.score != null || result.passResult));
}
function evaluatorResultLabel(result) {
    if (!result) {
        return '-';
    }
    return result.passResult || (result.score != null ? '已评分' : '-');
}
function evaluatorColumnLabel(evaluator) {
    return formatNameVersion(evaluator.evaluatorName, evaluator.versionName);
}
function evaluatorParamKey(param) {
    return `${param.paramId || ''}:${param.paramName || ''}`;
}
function findEvaluatorParam(row, evaluator, param) {
    const result = findEvaluatorResult(row, evaluator.taskEvaluatorId);
    if (!result?.params?.length) {
        return undefined;
    } else {
        return result.params.find((item) => evaluatorParamKey(item) === evaluatorParamKey(param));
    }
}
function evaluatorParamValue(row, evaluator, param) {
    return formatAgentOutputValue(findEvaluatorParam(row, evaluator, param)?.value) || '-';
}
function appOutputText(row) {
    const output = formatAppOutput(row.appOutput);
    if (row.appErrorMessage) {
        return [output, row.appErrorMessage].filter(Boolean).join('\n\n') || '-';
    } else {
        return output || '-';
    }
}
function evaluatorMessage(result) {
    if (!result) {
        return '';
    } else {
        return formatEvaluatorReason(result.resultValue || '') || result.errorMessage || '';
    }
}
function tagHeaderText(tag) {
    return `${tag?.tagName || '-'}（${tagTypeLabel(tag?.tagType)}）`;
}
async function refreshTagsAfterCreate() {
    await loadAllTags();
}
function metricPercent(value) {
    return value === undefined || value === null ? '-' : `${value}%`;
}
function metricNumber(value) {
    return value === undefined || value === null ? '-' : value;
}
function dimensionName(item) {
    return item?.displayName || item?.dimensionName || '-';
}
function stackSeries(name, field) {
    return {
        name,
        type: 'bar',
        stack: 'total',
        barWidth: 28,
        itemStyle: { borderRadius: name === '未完成' ? [6, 6, 0, 0] : 0 },
        data: metricDistributionDimensions.value.map((item) => item[field] ?? 0)
    };
}
function scoreTooltip(params) {
    const item = params?.[0];
    return item ? `${item.name}<br/>通过率：${item.value}%` : '';
}
function stackTooltip(params) {
    return (params ?? []).map((item) => `${item.marker}${item.seriesName}：${item.value}`).join('<br/>');
}
</script>

<template>
  <section class="task-detail-shell" v-loading="loading">
    <section class="task-detail-card" :class="{ 'is-metrics': activeTab === 'metrics' }">
      <div class="embedded-page-title">
        <nav class="page-breadcrumb" aria-label="页面路径">
          <button type="button" class="page-breadcrumb-link" @click="backToList">评测任务</button>
          <template v-if="base?.taskName">
            <span class="page-breadcrumb-separator">/</span>
            <OverflowTooltip :content="base.taskName" tag="span" class="page-breadcrumb-current" />
          </template>
        </nav>
        <div class="task-detail-tabs">
          <button
            type="button"
            :class="{ active: activeTab === 'data' }"
            @click="changeTaskDetailTab('data')"
          >
            数据明细
          </button>
          <button
            type="button"
            :class="{ active: activeTab === 'metrics' }"
            @click="changeTaskDetailTab('metrics')"
          >
            指标统计
          </button>
        </div>
      </div>

      <section v-if="activeTab === 'data'" class="task-basic-band">
        <h2>基础信息</h2>
        <div class="task-detail-info-grid">
          <div class="task-info-row task-info-row-primary">
            <div class="task-info-item">
              <span>评测集</span>
              <OverflowTooltip
                :content="formatNameVersion(base?.datasetName, base?.datasetVersionName)"
                tag="strong"
              />
            </div>
            <div class="task-info-item">
              <span>评测应用</span>
              <OverflowTooltip
                :content="formatAppBinding(base)"
                tag="strong"
              />
            </div>
            <div class="task-info-item">
              <span>创建人</span>
              <OverflowTooltip
                :content="base?.createdByName || '-'"
                tag="strong"
              />
            </div>
            <div class="task-info-item">
              <span>创建时间</span>
              <OverflowTooltip
                :content="formatTime(base?.createdDate)"
                tag="strong"
              />
            </div>
            <div class="task-info-item">
              <span>描述</span>
              <OverflowTooltip
                :content="base?.description || '暂无描述'"
                tag="strong"
              />
            </div>
          </div>
          <div class="task-dimension-section">
            <span class="task-dimension-title">评测维度</span>
            <div class="dimension-summary-row task-dimension-summary">
              <div v-if="evaluators.length" class="dimension-summary-group">
                <span class="dimension-summary-group-label">评估器</span>
                <el-tag
                  v-for="evaluator in evaluators"
                  :key="evaluator.taskEvaluatorId"
                  class="dimension-summary-pill"
                  type="info"
                  effect="light"
                >
                  <OverflowTooltip
                    :content="formatEvaluatorDimension(evaluator)"
                    class="dimension-summary-pill-text"
                  />
                </el-tag>
              </div>
              <div v-if="tags.length" class="dimension-summary-group">
                <span class="dimension-summary-group-label">标签</span>
                <el-tag
                  v-for="tag in tags"
                  :key="tag.taskTagId"
                  class="dimension-summary-pill"
                  type="info"
                  effect="light"
                >
                  <OverflowTooltip
                    :content="formatTagDimension(tag)"
                    class="dimension-summary-pill-text"
                  />
                </el-tag>
              </div>
              <span v-if="!evaluators.length && !tags.length" class="dimension-summary-empty">暂无评测维度</span>
            </div>
          </div>
        </div>
      </section>

      <section v-if="activeTab === 'data'" class="task-data-panel">
        <div class="panel-toolbar">
          <span class="meta">数据明细</span>
          <div class="table-toolbar-actions">
            <el-button :icon="PriceTag" @click="openTagDrawer">标签配置</el-button>
            <el-popover
              v-model:visible="columnSettingVisible"
              placement="bottom-end"
              :width="280"
              trigger="click"
              popper-class="column-setting-popover"
            >
              <template #reference>
                <el-button :icon="Operation">表头设置</el-button>
              </template>
              <div class="column-setting-panel">
                <div class="column-setting-head">
                  <strong>表头设置</strong>
                </div>
                <div class="column-setting-list">
                  <div
                    v-for="(column, index) in columnSettingItems"
                    :key="column.id"
                    class="column-setting-item"
                    draggable="true"
                    @dragstart="startColumnDrag(index)"
                    @dragenter.prevent="enterColumnDrag(index)"
                    @dragover.prevent
                    @dragend="finishColumnDrag"
                    @drop.prevent="finishColumnDrag"
                  >
                    <span class="column-drag-handle">☰</span>
                    <el-checkbox
                      :model-value="column.visible"
                      @change="setColumnVisible(column.id, Boolean($event))"
                    >
                      <OverflowTooltip
                        :content="column.settingLabel || column.label"
                        axis="horizontal"
                        class="column-setting-label"
                      />
                    </el-checkbox>
                  </div>
                </div>
                <div class="column-setting-actions">
                  <el-button link @click="resetColumnSettings">重置</el-button>
                  <el-button type="primary" @click="confirmColumnSettings">确定</el-button>
                </div>
              </div>
            </el-popover>
            <el-button class="toolbar-icon-button" :icon="Refresh" title="刷新" aria-label="刷新" @click="loadDetail()" />
            <el-button
              v-if="canStopTask"
              type="danger"
              plain
              :icon="CircleClose"
              :loading="stopping"
              :disabled="stopping"
              @click="stopTask"
            >
              停止任务
            </el-button>
          </div>
        </div>

        <el-table :data="rows" row-key="id" border height="100%" tooltip-effect="light" class="task-detail-table">
          <el-table-column label="状态" width="120" fixed="left" :resizable="false" align="center">
            <template #default="{ row }">
              <el-tooltip :content="statusLabel(row.status)" placement="top" effect="light">
                <el-icon class="task-status-icon" :class="statusIconClass(row.status)">
                  <component :is="statusIcon(row.status)" />
                </el-icon>
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column type="index" label="序号" width="90" fixed="left" :resizable="false" align="center" />
          <template v-for="column in visibleTableColumns" :key="column.id">
            <el-table-column
              v-if="column.type === 'field'"
              :label="column.label"
              min-width="220"
              :resizable="false"
            >
              <template #default="{ row }">
                <OverflowTooltip :content="row.values[column.refId || ''] || '-'" />
              </template>
            </el-table-column>
            <el-table-column
              v-else-if="column.type === 'appOutput'"
              label="应用输出"
              min-width="300"
              :resizable="false"
            >
              <template #default="{ row }">
                <OverflowTooltip
                  :content="appOutputText(row)"
                  tag="div"
                  class="app-output-preview"
                >
                  <span>{{ formatAppOutput(row.appOutput) || '-' }}</span>
                  <p v-if="row.appErrorMessage" class="task-error-preview">
                    {{ row.appErrorMessage }}
                  </p>
                </OverflowTooltip>
              </template>
            </el-table-column>
            <el-table-column
              v-else-if="column.type === 'evaluator'"
              :label="evaluatorColumnLabel(column.target)"
              :resizable="false"
              align="center"
            >
              <el-table-column v-for="param in column.target.params || []" :key="evaluatorParamKey(param)" :label="param.paramName" min-width="190" :resizable="false">
                <template #default="{ row }">
                  <OverflowTooltip
                    :content="evaluatorParamValue(row, column.target, param)"
                    class="param-value-preview"
                  />
                </template>
              </el-table-column>
              <el-table-column label="结果" width="92" :resizable="false" align="center">
                <template #default="{ row }">
                  <template v-if="isScoredEvaluatorResult(findEvaluatorResult(row, column.target.taskEvaluatorId))">
                    <el-tag :type="passTagType(findEvaluatorResult(row, column.target.taskEvaluatorId)?.passResult)" effect="plain">
                      <OverflowTooltip :content="evaluatorResultLabel(findEvaluatorResult(row, column.target.taskEvaluatorId))" />
                    </el-tag>
                  </template>
                  <el-tooltip v-else :content="statusLabel(findEvaluatorResult(row, column.target.taskEvaluatorId)?.status)" placement="top" effect="light">
                    <el-icon class="task-status-icon" :class="statusIconClass(findEvaluatorResult(row, column.target.taskEvaluatorId)?.status)">
                      <component :is="statusIcon(findEvaluatorResult(row, column.target.taskEvaluatorId)?.status)" />
                    </el-icon>
                  </el-tooltip>
                </template>
              </el-table-column>
              <el-table-column label="得分" width="86" :resizable="false" align="center">
                <template #default="{ row }">
                  <OverflowTooltip :content="findEvaluatorResult(row, column.target.taskEvaluatorId)?.score ?? '-'" />
                </template>
              </el-table-column>
              <el-table-column label="原因" min-width="260" :resizable="false">
                <template #default="{ row }">
                  <OverflowTooltip
                    :content="evaluatorMessage(findEvaluatorResult(row, column.target.taskEvaluatorId)) || '-'"
                    class="result-reason-preview"
                  />
                </template>
              </el-table-column>
            </el-table-column>
            <el-table-column
              v-else-if="column.type === 'tag'"
              width="170"
              :resizable="false"
              align="center"
            >
              <template #header>
                <div class="tag-table-header">
                  <OverflowTooltip :content="tagHeaderText(column.target)" class="tag-table-header-text" />
                  <el-button
                    class="tag-table-delete"
                    link
                    :icon="Delete"
                    :loading="tagOperatingIds.includes(column.target?.tagId || column.target?.taskTagId)"
                    :disabled="tagOperatingIds.includes(column.target?.tagId || column.target?.taskTagId)"
                    title="删除标签"
                    aria-label="删除标签"
                    @click.stop="removeTaskTag(column.target)"
                  />
                </div>
              </template>
              <template #default="{ row }">
                <div v-if="findTagResult(row, column.target.taskTagId)?.status === 'completed'" class="tag-result-cell">
                  <el-tag class="tag-result-status" :type="passTagType(findTagResult(row, column.target.taskTagId)?.passResult)" effect="plain">
                    <OverflowTooltip :content="findTagResult(row, column.target.taskTagId)?.passResult || '-'" />
                  </el-tag>
                  <span class="tag-result-separator">-</span>
                  <OverflowTooltip class="result-value tag-result-value" :content="tagResultValue(row, column.target.taskTagId)" />
                </div>
                <el-tag v-else type="info" effect="plain">
                  <OverflowTooltip content="待标注" />
                </el-tag>
              </template>
            </el-table-column>
          </template>
          <el-table-column label="操作" width="120" fixed="right" :resizable="false" align="center">
            <template #default="{ row }">
              <el-button link type="primary" @click="openAnnotation(row)">标注</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pager-row">
          <el-pagination
            v-model:current-page="page"
            v-model:page-size="size"
            :page-sizes="[5, 10, 20]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="total"
            @size-change="changeSize"
            @current-change="loadDetail"
          />
        </div>
      </section>
      <section v-else class="task-metrics-panel" v-loading="metricsLoading">
        <section class="metric-overview-grid">
          <article class="metric-card metric-score-card">
            <div class="metric-card-head">
              <h3>综合得分</h3>
              <span>{{ metricOverview?.scoredDimensionCount || 0 }} 个维度</span>
            </div>
            <EChartView class="metric-gauge-chart" :option="gaugeOption" />
          </article>
          <div class="metric-progress-grid">
            <article v-for="card in progressCards" :key="card.label" class="metric-card metric-progress-item">
              <span>{{ card.label }}</span>
              <strong>{{ card.value }}</strong>
            </article>
          </div>
        </section>

        <section class="metric-card metric-chart-card">
          <div class="metric-card-head">
            <h3>得分汇总</h3>
            <span>评估器 / 标签通过率</span>
          </div>
          <EChartView v-if="hasMetricDimensions" class="metric-bar-chart" :option="scoreChartOption" />
          <el-empty v-else description="暂无指标数据" />
        </section>

        <section class="metric-card metric-chart-card">
          <div class="metric-card-head">
            <h3>得分明细 · 数据项分布</h3>
            <el-select
              v-model="selectedDistributionId"
              class="metric-dimension-select"
              placeholder="选择维度"
            >
              <el-option
                v-for="item in metricDistributionDimensions"
                :key="item.dimensionId"
                :label="dimensionName(item)"
                :value="item.dimensionId"
              />
            </el-select>
          </div>
          <div v-if="hasMetricDimensions" class="metric-distribution-grid">
            <EChartView class="metric-stack-chart" :option="distributionStackOption" />
            <EChartView class="metric-pie-chart" :option="distributionPieOption" />
          </div>
          <el-empty v-else description="暂无分布数据" />
        </section>
      </section>
    </section>
    <TaskTagDrawer
      v-model="tagDrawerVisible"
      v-model:keyword="tagKeyword"
      v-model:tag-type="tagTypeFilter"
      v-model:page="tagPage"
      v-model:size="tagSize"
      title="标签配置"
      :tags="allTags"
      :selected-tag-ids="selectedTagIds"
      :tag-type-options="tagTypeOptions"
      :loading="tagLoading"
      :operating-tag-ids="tagOperatingIds"
      :total="tagTotal"
      @refresh="loadAllTags"
      @search="searchAllTags"
      @page-change="loadAllTags"
      @size-change="changeTagSize"
      @create="tagCreateVisible = true"
      @add="addTaskTag"
      @remove="removeTaskTagByTag"
    />
    <TagCreateDialog v-model="tagCreateVisible" @created="refreshTagsAfterCreate" />
  </section>
</template>
