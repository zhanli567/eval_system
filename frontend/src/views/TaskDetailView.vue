<script setup>
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import { Back, CircleCheck, CircleClose, Clock, Loading, Refresh, VideoPlay } from '@element-plus/icons-vue';
import { useTaskDetail } from '../modules/task/composables/useTaskDetail';
import { formatAgentOutputValue, formatAppOutput, formatEvaluatorReason } from '../utils/taskDisplay';
const route = useRoute();
const taskId = computed(() => String(route.params.taskId ?? ''));
const { loading, starting, stopping, page, size, base, fields, evaluators, tags, rows, total, canStartTask, canStopTask, loadDetail, backToList, startTask, stopTask, openItemDetail, openAnnotation, canAnnotateItem, annotationDisabledReason, changeSize, formatAppBinding, statusLabel, passTagType, tagTypeLabel, formatTime } = useTaskDetail(taskId);
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
</script>

<template>
  <header class="topbar detail-topbar">
    <div>
      <el-button link type="primary" :icon="Back" class="back-link" @click="backToList">返回评测任务列表</el-button>
      <h1>
        {{ base?.taskName || '评测任务详情' }}
        <el-tooltip v-if="base" :content="statusLabel(base.status)" placement="top" effect="light">
          <el-icon class="task-status-icon task-title-status" :class="statusIconClass(base.status)">
            <component :is="statusIcon(base.status)" />
          </el-icon>
        </el-tooltip>
      </h1>
    </div>
    <div class="top-actions">
      <el-button :icon="Refresh" @click="loadDetail">刷新</el-button>
      <el-button
        v-if="canStartTask"
        type="primary"
        :icon="VideoPlay"
        :loading="starting"
        :disabled="starting"
        @click="startTask"
      >
        开始
      </el-button>
      <el-button
        v-if="canStopTask"
        type="danger"
        :icon="CircleClose"
        :loading="stopping"
        :disabled="stopping"
        @click="stopTask"
      >
        停止任务
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

    <section class="task-data-panel">
      <div class="panel-toolbar">
        <span class="meta">数据明细</span>
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
        <el-table-column type="index" label="序号" width="90" fixed="left" align="center" />
        <el-table-column :label="formatNameVersion(base?.datasetName, base?.datasetVersionName)" align="center">
          <el-table-column v-for="field in fields" :key="field.id" :label="field.fieldName" min-width="220">
            <template #default="{ row }">
              <OverflowTooltip :content="row.values[field.id || ''] || '-'" />
            </template>
          </el-table-column>
        </el-table-column>
        <el-table-column label="应用输出" min-width="260">
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
        <el-table-column v-for="evaluator in evaluators" :key="evaluator.taskEvaluatorId" :label="evaluatorColumnLabel(evaluator)" align="center">
          <el-table-column v-for="param in evaluator.params || []" :key="evaluatorParamKey(param)" :label="param.paramName" min-width="180">
            <template #default="{ row }">
              <OverflowTooltip
                :content="evaluatorParamValue(row, evaluator, param)"
                class="param-value-preview"
              />
            </template>
          </el-table-column>
          <el-table-column label="结果" width="120">
            <template #default="{ row }">
              <template v-if="isScoredEvaluatorResult(findEvaluatorResult(row, evaluator.taskEvaluatorId))">
                <el-tag :type="passTagType(findEvaluatorResult(row, evaluator.taskEvaluatorId)?.passResult)" effect="plain">
                  {{ evaluatorResultLabel(findEvaluatorResult(row, evaluator.taskEvaluatorId)) }}
                </el-tag>
              </template>
              <el-tooltip v-else :content="statusLabel(findEvaluatorResult(row, evaluator.taskEvaluatorId)?.status)" placement="top" effect="light">
                <el-icon class="task-status-icon" :class="statusIconClass(findEvaluatorResult(row, evaluator.taskEvaluatorId)?.status)">
                  <component :is="statusIcon(findEvaluatorResult(row, evaluator.taskEvaluatorId)?.status)" />
                </el-icon>
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column label="得分" width="110">
            <template #default="{ row }">{{ findEvaluatorResult(row, evaluator.taskEvaluatorId)?.score ?? '-' }}</template>
          </el-table-column>
          <el-table-column label="原因" min-width="240">
            <template #default="{ row }">
              <OverflowTooltip
                :content="evaluatorMessage(findEvaluatorResult(row, evaluator.taskEvaluatorId)) || '-'"
                class="result-reason-preview"
              />
            </template>
          </el-table-column>
        </el-table-column>
        <el-table-column v-if="tags.length" label="标签" align="center">
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
              <el-tag v-else-if="findTagResult(row, tag.taskTagId)?.status === 'stopped'" type="info" effect="plain">已中止</el-tag>
              <el-tag v-else type="info" effect="plain">未标注</el-tag>
            </template>
          </el-table-column>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right" :resizable="false" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openItemDetail(row)">详情</el-button>
            <el-tooltip
              :content="annotationDisabledReason(row)"
              placement="top"
              effect="light"
              :disabled="canAnnotateItem(row)"
            >
              <span class="task-action-button-wrap">
                <el-button link type="primary" :disabled="!canAnnotateItem(row)" @click="openAnnotation(row)">标注</el-button>
              </span>
            </el-tooltip>
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
