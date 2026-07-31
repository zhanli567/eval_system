<script setup>
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import { ArrowLeft, ArrowRight, Back } from '@element-plus/icons-vue';
import { useTaskAnnotation } from '../modules/task/composables/useTaskAnnotation';
import { compactText, formatAppOutput, formatEvaluatorReason } from '../utils/taskDisplay';
const route = useRoute();
const taskId = computed(() => String(route.params.taskId ?? ''));
const taskItemId = computed(() => String(route.params.taskItemId ?? ''));
const { loading, saving, loadError, form, task, item, fields, tags, evaluators, previousItemId, nextItemId, saveAnnotation, backToDetail, goItem, passTagType, tagTypeLabel, optionLabel, appOutputEmptyDescription } = useTaskAnnotation(taskId, taskItemId);
const formattedAppOutput = computed(() => formatAppOutput(item.value?.appOutput || ''));
function formatNameVersion(name, version) {
    return `${name || '-'} / ${version || '-'}`;
}
function evaluatorResultLabel(result) {
    return result.passResult || result.status || '-';
}
function evaluatorReason(result) {
    return formatEvaluatorReason(result.resultValue || '') || result.errorMessage || '';
}
function evaluatorParamSource(param) {
    const sourceType = param.sourceType === 'dataset_field' ? '评测集字段' : '应用输出';
    return `${sourceType}：${param.sourceName || '-'}`;
}
</script>

<template>
  <header class="topbar detail-topbar">
    <div>
      <el-button link type="primary" :icon="Back" class="back-link" @click="backToDetail">返回评测任务详情</el-button>
      <h1>{{ task?.taskName || '标注' }} · 第 {{ item?.rowNo || '-' }} 条</h1>
    </div>
    <div class="top-actions">
      <el-button :disabled="!previousItemId" :icon="ArrowLeft" @click="goItem(previousItemId)">上一条</el-button>
      <el-button :disabled="!nextItemId" @click="goItem(nextItemId)">
        下一条
        <el-icon class="el-icon--right"><ArrowRight /></el-icon>
      </el-button>
      <el-button type="primary" :loading="saving" :disabled="!!loadError || !item || !tags.length" @click="saveAnnotation">保存标注</el-button>
    </div>
  </header>

  <section class="annotation-shell" v-loading="loading">
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
    <aside class="annotation-column">
      <h2>评测集数据</h2>
      <div class="annotation-field-list">
        <div v-for="field in fields" :key="field.id" class="annotation-field">
          <span>{{ field.fieldName }}</span>
          <p>{{ item?.values[field.id || ''] || '-' }}</p>
        </div>
        <el-empty v-if="!fields.length" description="暂无评测集字段" :image-size="72" />
      </div>
    </aside>

    <main class="annotation-output">
      <h2>评估器及其数据</h2>
      <section class="annotation-subsection">
        <h3>应用输出</h3>
        <div class="app-output-box">
          <p v-if="formattedAppOutput">{{ formattedAppOutput }}</p>
          <el-empty v-else :description="appOutputEmptyDescription()" :image-size="80" />
        </div>
      </section>

      <div class="auto-result-list evaluator-data-list">
        <div v-for="result in evaluators" :key="result.id" class="auto-result-item evaluator-data-item">
          <div class="evaluator-data-head">
            <div>
              <strong>{{ formatNameVersion(result.evaluatorName, result.versionName) }}</strong>
              <span>{{ result.evaluatorType || '-' }}</span>
            </div>
            <el-tag :type="passTagType(result.passResult)" effect="plain">
              {{ evaluatorResultLabel(result) }}
            </el-tag>
            <span class="result-value">得分 {{ result.score ?? '-' }}</span>
          </div>
          <div v-if="result.params?.length" class="evaluator-param-list">
            <div v-for="param in result.params" :key="`${param.paramId || ''}:${param.paramName || ''}`" class="evaluator-param-row">
              <span>
                {{ param.paramName }}
                <em>{{ evaluatorParamSource(param) }}</em>
              </span>
              <p>{{ param.value || '-' }}</p>
            </div>
          </div>
          <el-empty v-else description="暂无参数映射" :image-size="64" />
          <p v-if="evaluatorReason(result)" class="auto-result-reason">
            {{ compactText(evaluatorReason(result), 360) }}
          </p>
        </div>
        <el-empty v-if="!evaluators.length" description="暂无自动评估结果" :image-size="72" />
      </div>
    </main>

    <aside class="annotation-column annotation-form-column">
      <h2>标注区域</h2>
      <el-form label-position="top">
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
        <el-empty v-if="!tags.length" description="当前任务暂无人工标签" :image-size="72" />
      </el-form>
    </aside>
    </template>
  </section>
</template>
