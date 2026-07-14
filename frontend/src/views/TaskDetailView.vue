<script setup>
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import { Back, CircleCheck, CircleClose, Clock, Loading, Refresh, VideoPlay } from '@element-plus/icons-vue';
import { useTaskDetail } from '../modules/task/composables/useTaskDetail';
import { compactText, formatAppOutput, formatEvaluatorReason } from '../utils/taskDisplay';
const route = useRoute();
const taskId = computed(() => String(route.params.taskId ?? ''));
const { loading, starting, page, size, base, fields, evaluators, tags, rows, total, loadDetail, backToList, startTask, openAnnotation, changeSize, formatAppBinding, statusLabel, passTagType, tagTypeLabel, formatTime } = useTaskDetail(taskId);
const statusIcons = {
    pending: Clock,
    running: Loading,
    completed: CircleCheck,
    failed: CircleClose,
    annotation_pending: Clock,
    annotating: Loading,
    skipped: Clock
};
function statusIcon(value) {
    return statusIcons[value] || Clock;
}
function statusIconClass(value) {
    return `is-${value || 'pending'}`;
}
function findTagResult(row, taskTagId) {
    return row.tagResults.find((item) => item.taskTagId === taskTagId);
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
    if (!result)
        return '-';
    return result.passResult || (result.score != null ? '已评分' : '-');
}
</script>

<template>
  <header class="topbar detail-topbar">
    <div>
      <el-button link type="primary" :icon="Back" class="back-link" @click="backToList">返回评测任务列表</el-button>
      <h1>
        {{ base?.taskName || '评测任务详情' }}
        <el-tooltip v-if="base" :content="statusLabel(base.status)" placement="top">
          <el-icon class="task-status-icon task-title-status" :class="statusIconClass(base.status)">
            <component :is="statusIcon(base.status)" />
          </el-icon>
        </el-tooltip>
      </h1>
    </div>
    <div class="top-actions">
      <el-button :icon="Refresh" @click="loadDetail">刷新</el-button>
      <el-button
        v-if="base?.status === 'pending' || base?.status === 'failed'"
        type="primary"
        :icon="VideoPlay"
        :loading="starting"
        :disabled="starting"
        @click="startTask"
      >
        开始
      </el-button>
    </div>
  </header>

  <section class="task-detail-shell" v-loading="loading">
    <section class="task-basic-band">
      <h2>基础信息</h2>
      <div class="task-detail-info-grid">
        <div class="task-info-row task-info-row-primary">
          <div class="task-info-item">
            <span>评测集</span>
            <el-tooltip :content="formatNameVersion(base?.datasetName, base?.datasetVersionName)" placement="top">
              <strong>{{ formatNameVersion(base?.datasetName, base?.datasetVersionName) }}</strong>
            </el-tooltip>
          </div>
          <div class="task-info-item">
            <span>评测应用</span>
            <el-tooltip :content="formatAppBinding(base)" placement="top">
              <strong>{{ formatAppBinding(base) }}</strong>
            </el-tooltip>
          </div>
          <div class="task-info-item">
            <span>创建人</span>
            <el-tooltip :content="base?.createdByName || '-'" placement="top">
              <strong>{{ base?.createdByName || '-' }}</strong>
            </el-tooltip>
          </div>
          <div class="task-info-item">
            <span>创建时间</span>
            <el-tooltip :content="formatTime(base?.createdDate)" placement="top">
              <strong>{{ formatTime(base?.createdDate) }}</strong>
            </el-tooltip>
          </div>
          <div class="task-info-item">
            <span>描述</span>
            <el-tooltip :content="base?.description || '暂无描述'" placement="top">
              <strong>{{ base?.description || '暂无描述' }}</strong>
            </el-tooltip>
          </div>
        </div>
        <div class="task-dimension-section">
          <span class="task-dimension-title">评测维度</span>
          <div class="dimension-summary-row task-dimension-summary">
            <div v-if="evaluators.length" class="dimension-summary-group">
              <span class="dimension-summary-group-label">评估器</span>
              <el-tooltip v-for="evaluator in evaluators" :key="evaluator.taskEvaluatorId" :content="formatEvaluatorDimension(evaluator)" placement="top">
                <el-tag class="dimension-summary-pill" type="info" effect="light">
                  {{ formatEvaluatorDimension(evaluator) }}
                </el-tag>
              </el-tooltip>
            </div>
            <div v-if="tags.length" class="dimension-summary-group">
              <span class="dimension-summary-group-label">标签</span>
              <el-tooltip v-for="tag in tags" :key="tag.taskTagId" :content="formatTagDimension(tag)" placement="top">
                <el-tag class="dimension-summary-pill" type="info" effect="light">
                  {{ formatTagDimension(tag) }}
                </el-tag>
              </el-tooltip>
            </div>
            <span v-if="!evaluators.length && !tags.length" class="dimension-summary-empty">暂无评测维度</span>
          </div>
        </div>
      </div>
    </section>

    <section class="task-data-panel">
      <div class="panel-toolbar">
        <span class="meta">数据明细</span>
      </div>

      <el-table :data="rows" row-key="id" border height="100%" tooltip-effect="light" class="task-detail-table">
        <el-table-column label="状态" width="120" fixed="left" :resizable="false">
          <template #default="{ row }">
            <el-tooltip :content="statusLabel(row.status)" placement="top">
              <el-icon class="task-status-icon" :class="statusIconClass(row.status)">
                <component :is="statusIcon(row.status)" />
              </el-icon>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column type="index" label="序号" width="90" fixed="left" />
        <el-table-column v-for="field in fields" :key="field.id" :label="field.fieldName" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ row.values[field.id || ''] || '-' }}</template>
        </el-table-column>
        <el-table-column label="应用输出" min-width="260" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="app-output-preview">{{ compactText(formatAppOutput(row.appOutput)) || '-' }}</div>
            <p v-if="row.appErrorMessage" class="task-error-preview">
              {{ compactText(row.appErrorMessage, 120) }}
            </p>
          </template>
        </el-table-column>
        <el-table-column v-for="tag in tags" :key="tag.taskTagId" :label="tag.tagName" min-width="190">
          <template #default="{ row }">
            <template v-if="findTagResult(row, tag.taskTagId)?.status === 'completed'">
              <el-tag :type="passTagType(findTagResult(row, tag.taskTagId)?.passResult)" effect="plain">
                {{ findTagResult(row, tag.taskTagId)?.passResult || '-' }}
              </el-tag>
              <span class="result-value">
                {{
                  findTagResult(row, tag.taskTagId)?.optionName ||
                  findTagResult(row, tag.taskTagId)?.valueText ||
                  findTagResult(row, tag.taskTagId)?.valueNumber ||
                  '-'
                }}
              </span>
            </template>
            <el-tag v-else type="info" effect="plain">未标注</el-tag>
          </template>
        </el-table-column>
        <el-table-column v-for="evaluator in evaluators" :key="evaluator.taskEvaluatorId" :label="evaluator.evaluatorName" min-width="190">
          <template #default="{ row }">
            <template v-if="isScoredEvaluatorResult(findEvaluatorResult(row, evaluator.taskEvaluatorId))">
              <el-tag :type="passTagType(findEvaluatorResult(row, evaluator.taskEvaluatorId)?.passResult)" effect="plain">
                {{ evaluatorResultLabel(findEvaluatorResult(row, evaluator.taskEvaluatorId)) }}
              </el-tag>
              <span class="result-value">
                {{ findEvaluatorResult(row, evaluator.taskEvaluatorId)?.score ?? '-' }}
              </span>
              <p v-if="formatEvaluatorReason(findEvaluatorResult(row, evaluator.taskEvaluatorId)?.resultValue)" class="result-reason-preview">
                {{ compactText(formatEvaluatorReason(findEvaluatorResult(row, evaluator.taskEvaluatorId)?.resultValue), 96) }}
              </p>
            </template>
            <el-tooltip v-else :content="statusLabel(findEvaluatorResult(row, evaluator.taskEvaluatorId)?.status)" placement="top">
              <el-icon class="task-status-icon" :class="statusIconClass(findEvaluatorResult(row, evaluator.taskEvaluatorId)?.status)">
                <component :is="statusIcon(findEvaluatorResult(row, evaluator.taskEvaluatorId)?.status)" />
              </el-icon>
            </el-tooltip>
            <p v-if="findEvaluatorResult(row, evaluator.taskEvaluatorId)?.errorMessage" class="task-error-preview">
              {{ compactText(findEvaluatorResult(row, evaluator.taskEvaluatorId)?.errorMessage, 120) }}
            </p>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right" :resizable="false">
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
  </section>
</template>
