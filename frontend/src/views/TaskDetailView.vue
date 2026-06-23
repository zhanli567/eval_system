<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { Back, Refresh, VideoPlay } from '@element-plus/icons-vue'
import { useTaskDetail } from '../modules/task/composables/useTaskDetail'
import { compactText, formatAppOutput, formatEvaluatorReason } from '../utils/taskDisplay'
import type { TaskBase, TaskEvaluatorResult, TaskItemDetail } from '../types'

const route = useRoute()
const taskId = computed(() => String(route.params.taskId ?? ''))

const {
  loading,
  starting,
  page,
  size,
  base,
  fields,
  evaluators,
  tags,
  rows,
  total,
  loadDetail,
  backToList,
  startTask,
  openAnnotation,
  statusLabel,
  statusTagType,
  passTagType,
  tagTypeLabel,
  formatTime
} = useTaskDetail(taskId)

function findTagResult(row: TaskItemDetail, taskTagId: string) {
  return row.tagResults.find((item) => item.taskTagId === taskTagId)
}

function findEvaluatorResult(row: TaskItemDetail, taskEvaluatorId: string) {
  return row.evaluatorResults.find((item) => item.taskEvaluatorId === taskEvaluatorId)
}

function formatAppBinding(task?: TaskBase | null) {
  if (!task || task.appType !== 'agent') return '-'
  const appId = task.appId || '智能体应用'
  return task.appAgentAlias ? `${appId} / ${task.appAgentAlias}` : appId
}

function isScoredEvaluatorResult(result?: TaskEvaluatorResult) {
  return Boolean(result && (result.status === 'completed' || result.score != null || result.passResult))
}

function evaluatorResultLabel(result?: TaskEvaluatorResult) {
  if (!result) return '-'
  return result.passResult || (result.score != null ? '已评分' : '-')
}
</script>

<template>
  <header class="topbar detail-topbar">
    <div>
      <el-button link type="primary" :icon="Back" class="back-link" @click="backToList">返回评测任务列表</el-button>
      <p class="eyebrow">应用评测 / 评测任务详情</p>
      <h1>
        {{ base?.taskName || '评测任务详情' }}
        <el-tag v-if="base" :type="statusTagType(base.status)" effect="plain">{{ statusLabel(base.status) }}</el-tag>
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
      <div class="task-basic-grid">
        <div>
          <span>评测集</span>
          <strong>{{ base?.datasetName || '-' }} {{ base?.datasetVersionName || '' }}</strong>
        </div>
        <div>
          <span>评测应用</span>
          <strong>{{ formatAppBinding(base) }}</strong>
        </div>
        <div>
          <span>创建时间</span>
          <strong>{{ formatTime(base?.createdDate) }}</strong>
        </div>
        <div>
          <span>更新时间</span>
          <strong>{{ formatTime(base?.lastUpdatedDate) }}</strong>
        </div>
        <div>
          <span>描述</span>
          <strong>{{ base?.description || '暂无描述' }}</strong>
        </div>
      </div>
      <div class="dimension-summary-row">
        <span class="dimension-summary-label">评测维度</span>
        <div v-if="evaluators.length" class="dimension-summary-group">
          <strong>评估器</strong>
          <el-tag v-for="evaluator in evaluators" :key="evaluator.taskEvaluatorId" class="dimension-summary-pill" type="info" effect="light">
            {{ evaluator.evaluatorName }}
          </el-tag>
        </div>
        <div v-if="tags.length" class="dimension-summary-group">
          <strong>标签</strong>
          <el-tag v-for="tag in tags" :key="tag.taskTagId" class="dimension-summary-pill" type="info" effect="light">
            {{ tag.tagName }}（{{ tagTypeLabel(tag.tagType) }}）
          </el-tag>
        </div>
        <span v-if="!evaluators.length && !tags.length" class="dimension-summary-empty">暂无评测维度</span>
      </div>
    </section>

    <section class="task-data-panel">
      <div class="panel-toolbar">
        <span class="meta">数据明细</span>
      </div>

      <el-table :data="rows" row-key="id" border height="100%" tooltip-effect="light" class="task-detail-table">
        <el-table-column label="状态" width="120" fixed="left">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="plain">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="序号" width="90" fixed="left">
          <template #default="{ row }"># {{ row.rowNo }}</template>
        </el-table-column>
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
            <el-tag v-else :type="statusTagType(findEvaluatorResult(row, evaluator.taskEvaluatorId)?.status)" effect="plain">
              {{ statusLabel(findEvaluatorResult(row, evaluator.taskEvaluatorId)?.status) }}
            </el-tag>
            <p v-if="findEvaluatorResult(row, evaluator.taskEvaluatorId)?.errorMessage" class="task-error-preview">
              {{ compactText(findEvaluatorResult(row, evaluator.taskEvaluatorId)?.errorMessage, 120) }}
            </p>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openAnnotation(row)">标注</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager-row">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          layout="total, prev, pager, next"
          :total="total"
          @current-change="loadDetail"
        />
      </div>
    </section>
  </section>
</template>
