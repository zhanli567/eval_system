<script setup>
import { computed, ref } from 'vue';
import { useRoute } from 'vue-router';
import {
    ArrowDown,
    ArrowLeft,
    ArrowRight,
    ArrowUp,
    ChatLineRound,
    CircleCheckFilled,
    Collection,
    DataAnalysis,
    PriceTag
} from '@element-plus/icons-vue';
import { useTaskAnnotation } from '../modules/task/composables/useTaskAnnotation';
import { formatAppOutput, formatEvaluatorReason } from '../utils/taskDisplay';
import { statusLabel } from '../utils/taskLabels';
import { renderSafeMarkdown } from '../utils/markdownRenderer';
const route = useRoute();
const taskId = computed(() => String(route.params.taskId ?? ''));
const taskItemId = computed(() => String(route.params.taskItemId ?? ''));
const readonlyMode = computed(() => route.query.mode === 'detail');
const { loading, saving, loadError, form, task, item, fields, tags, evaluators, previousItemId, nextItemId, saveAnnotation, backToDetail, goItem, passTagType, tagTypeLabel, optionLabel, appOutputEmptyDescription } = useTaskAnnotation(taskId, taskItemId, readonlyMode);
const formattedAppOutput = computed(() => formatAppOutput(item.value?.appOutput || ''));
const renderedAppOutput = computed(() => renderSafeMarkdown(formattedAppOutput.value));
const hasAppOutput = computed(() => task.value?.appType === 'agent' && Boolean(task.value?.appId));
const completedTagCount = computed(() => tags.value.filter((tag) => tagHasAnnotation(tag)).length);
const evaluatorPanelExpanded = ref(false);
const EVALUATOR_STATUS_LABELS = {
    completed: '完成',
    failed: '失败',
    stopped: '终止',
    pending: '待执行',
    running: '进行中',
    skipped: '跳过'
};
const PASS_RESULT_LABELS = {
    pass: 'Pass',
    fail: 'Fail'
};
function formatNameVersion(name, version) {
    return `${name || '-'} / ${version || '-'}`;
}
function evaluatorReason(result) {
    return formatEvaluatorReason(result.resultValue || '') || result.errorMessage || '';
}
function evaluatorScoreLabel(result) {
    return result.score === undefined || result.score === null ? '-' : result.score;
}
function evaluatorStatusText(result) {
    return EVALUATOR_STATUS_LABELS[result?.status] || statusLabel(result?.status);
}
function evaluatorStatusClass(result) {
    return `is-${result?.status || 'pending'}`;
}
function evaluatorPassScoreLabel(result) {
    const resultText = PASS_RESULT_LABELS[result?.passResult] || '-';
    const scoreText = evaluatorScoreLabel(result);
    return resultText === '-' ? '-' : `${resultText}: ${scoreText}`;
}
function evaluatorPassScoreClass(result) {
    return `is-${result?.passResult || 'empty'}`;
}
function toggleEvaluatorPanel() {
    evaluatorPanelExpanded.value = !evaluatorPanelExpanded.value;
}
function tagHasAnnotation(tag) {
    const value = form[tag.taskTagId];
    if (tag.tagType === 'number') {
        return value !== undefined && value !== null && value !== '';
    } else {
        return Boolean(String(value ?? '').trim());
    }
}
</script>

<template>
  <section class="annotation-shell" :class="{ 'annotation-shell--without-app': !hasAppOutput }" v-loading="loading">
    <div class="embedded-page-title">
      <nav class="page-breadcrumb" aria-label="页面路径">
        <button type="button" class="page-breadcrumb-link" @click="backToDetail">评测任务详情</button>
        <span class="page-breadcrumb-separator">/</span>
        <span class="page-breadcrumb-current">标注</span>
      </nav>
    </div>

    <el-alert
      v-if="loadError"
      class="annotation-load-error"
      type="error"
      show-icon
      :closable="false"
      title="标注数据加载失败"
      :description="loadError"
    />
    <el-empty v-else-if="!item" class="annotation-empty-state" description="暂无可标注数据" />
    <template v-else>
      <aside class="annotation-pane annotation-dataset-pane">
        <div class="annotation-section-title">
          <el-icon><Collection /></el-icon>
          <span>评测集数据</span>
        </div>
        <div class="annotation-field-list">
          <div v-for="field in fields" :key="field.id" class="annotation-field">
            <span>{{ field.fieldName }}</span>
            <p>{{ item?.values[field.id || ''] || '-' }}</p>
          </div>
          <el-empty v-if="!fields.length" description="暂无评测集字段" :image-size="72" />
        </div>
      </aside>

      <main v-if="hasAppOutput" class="annotation-pane annotation-app-pane">
        <div class="annotation-section-title">
          <el-icon><ChatLineRound /></el-icon>
          <span>应用输出</span>
        </div>
        <div class="app-output-box">
          <div v-if="formattedAppOutput" class="markdown-content" v-html="renderedAppOutput"></div>
          <el-empty v-else :description="appOutputEmptyDescription()" :image-size="80" />
        </div>
      </main>

      <aside class="annotation-pane annotation-side-panel">
        <section class="annotation-content-block annotation-form-section">
          <div class="annotation-section-title">
            <el-icon><PriceTag /></el-icon>
            <span>标签（人工标注）</span>
            <small v-if="tags.length" class="annotation-complete-count">
              <el-icon><CircleCheckFilled /></el-icon>
              标注完成：{{ completedTagCount }} / {{ tags.length }}
            </small>
          </div>
          <el-empty
            v-if="!tags.length"
            class="annotation-tag-empty"
            description="暂无数据，请在添加标签后进行标注操作"
            :image-size="180"
          />
          <el-form v-else class="annotation-tag-form" label-position="top" :disabled="readonlyMode">
            <div v-for="tag in tags" :key="tag.taskTagId" class="annotation-tag-editor">
              <div class="annotation-tag-head">
                <strong>{{ tag.tagName }}</strong>
                <el-tag size="small" effect="plain">{{ tagTypeLabel(tag.tagType) }}</el-tag>
              </div>
              <span class="hint">{{ optionLabel(tag) }}</span>

              <el-input
                v-if="tag.tagType === 'text'"
                v-model="form[tag.taskTagId]"
                type="textarea"
                :autosize="{ minRows: 3, maxRows: 6 }"
                placeholder="请输入标注内容"
              />
              <el-input-number
                v-else-if="tag.tagType === 'number'"
                v-model="form[tag.taskTagId]"
                :min="tag.minValue"
                :max="tag.maxValue"
                controls-position="right"
                class="wide-control"
              />
              <el-radio-group v-else-if="tag.tagType === 'boolean'" v-model="form[tag.taskTagId]" class="option-radio-group">
                <el-radio-button v-for="option in tag.options" :key="option.id" :label="option.id">
                  {{ option.optionName }}
                </el-radio-button>
              </el-radio-group>
              <el-select v-else v-model="form[tag.taskTagId]" clearable placeholder="请选择分类" class="wide-control">
                <el-option
                  v-for="option in tag.options"
                  :key="option.id"
                  :label="`${option.optionName} · ${option.optionGroup === 'pass' ? 'Pass' : 'Fail'}`"
                  :value="option.id"
                />
              </el-select>

              <div v-if="tag.result?.status === 'completed'" class="annotation-current-result">
                <el-tag :type="passTagType(tag.result.passResult)" effect="plain">{{ tag.result.passResult }}</el-tag>
                <span>已标注</span>
              </div>
            </div>
          </el-form>
        </section>

        <section
          class="annotation-content-block annotation-evaluator-block"
          :class="{ 'is-expanded': evaluatorPanelExpanded }"
        >
          <button type="button" class="annotation-evaluator-toggle" @click="toggleEvaluatorPanel">
            <span class="annotation-section-title">
              <el-icon><DataAnalysis /></el-icon>
              <span>评估器（自动）</span>
            </span>
            <el-icon class="annotation-evaluator-toggle-icon">
              <ArrowUp v-if="evaluatorPanelExpanded" />
              <ArrowDown v-else />
            </el-icon>
          </button>
          <div v-show="evaluatorPanelExpanded" class="auto-result-list evaluator-data-list">
            <article v-for="result in evaluators" :key="result.id" class="annotation-evaluator-card">
              <header class="annotation-evaluator-head">
                <div class="annotation-evaluator-title">
                  <strong>{{ formatNameVersion(result.evaluatorName, result.versionName) }}</strong>
                </div>
                <div class="annotation-evaluator-summary">
                  <span class="annotation-status-chip">
                    <i class="annotation-status-dot" :class="evaluatorStatusClass(result)"></i>
                    {{ evaluatorStatusText(result) }}
                  </span>
                  <span v-if="result.passResult" class="annotation-score-pill" :class="evaluatorPassScoreClass(result)">
                    {{ evaluatorPassScoreLabel(result) }}
                  </span>
                </div>
              </header>

              <section v-if="evaluatorReason(result)" class="annotation-evaluator-reason">
                <span class="annotation-evaluator-reason-label">原因</span>
                <OverflowTooltip
                  :content="evaluatorReason(result)"
                  tag="p"
                  class="annotation-evaluator-reason-text"
                />
              </section>
            </article>
            <el-empty v-if="!evaluators.length" description="暂无自动评估结果" :image-size="72" />
          </div>
        </section>

        <footer class="annotation-bottom-actions">
          <span class="annotation-item-counter">第 {{ item?.rowNo || '-' }} 条</span>
          <el-button :disabled="!previousItemId || saving" :icon="ArrowLeft" @click="goItem(previousItemId)">上一条</el-button>
          <el-button :disabled="!nextItemId || saving" @click="goItem(nextItemId)">
            下一条
            <el-icon class="el-icon--right"><ArrowRight /></el-icon>
          </el-button>
          <el-button v-if="!readonlyMode && tags.length" type="primary" :loading="saving" :disabled="saving || !!loadError || !item" @click="saveAnnotation">保存标注</el-button>
        </footer>
      </aside>
    </template>
  </section>
</template>
