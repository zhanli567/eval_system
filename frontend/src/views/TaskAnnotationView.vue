<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft, ArrowRight, Back } from '@element-plus/icons-vue'
import { useTaskAnnotation } from '../modules/task/composables/useTaskAnnotation'

const route = useRoute()
const taskId = computed(() => String(route.params.taskId ?? ''))
const taskItemId = computed(() => String(route.params.taskItemId ?? ''))

const {
  loading,
  saving,
  form,
  task,
  item,
  fields,
  tags,
  evaluators,
  previousItemId,
  nextItemId,
  saveAnnotation,
  backToDetail,
  goItem,
  passTagType,
  tagTypeLabel,
  optionLabel
} = useTaskAnnotation(taskId, taskItemId)
</script>

<template>
  <header class="topbar detail-topbar">
    <div>
      <el-button link type="primary" :icon="Back" class="back-link" @click="backToDetail">返回评测任务详情</el-button>
      <p class="eyebrow">人工标注</p>
      <h1>{{ task?.taskName || '标注' }} · #{{ item?.rowNo || '-' }}</h1>
    </div>
    <div class="top-actions">
      <el-button :disabled="!previousItemId" :icon="ArrowLeft" @click="goItem(previousItemId)">上一条</el-button>
      <el-button :disabled="!nextItemId" @click="goItem(nextItemId)">
        下一条
        <el-icon class="el-icon--right"><ArrowRight /></el-icon>
      </el-button>
      <el-button type="primary" :loading="saving" @click="saveAnnotation">保存标注</el-button>
    </div>
  </header>

  <section class="annotation-shell" v-loading="loading">
    <aside class="annotation-column">
      <h2>评测集数据</h2>
      <div class="annotation-field-list">
        <div v-for="field in fields" :key="field.id" class="annotation-field">
          <span>{{ field.fieldName }}</span>
          <p>{{ item?.values[field.id || ''] || '-' }}</p>
        </div>
      </div>
    </aside>

    <main class="annotation-output">
      <h2>应用输出</h2>
      <div class="app-output-box">
        <p v-if="item?.appOutput">{{ item.appOutput }}</p>
        <el-empty v-else description="当前任务未关联应用，无应用输出" :image-size="80" />
      </div>

      <h2>评估器（自动）</h2>
      <div class="auto-result-list">
        <div v-for="result in evaluators" :key="result.id" class="auto-result-item">
          <div>
            <strong>{{ result.evaluatorName }}</strong>
            <span>{{ result.versionName }}</span>
          </div>
          <el-tag :type="passTagType(result.passResult)" effect="plain">
            {{ result.passResult || result.status }}
          </el-tag>
          <span class="result-value">得分 {{ result.score ?? '-' }}</span>
        </div>
      </div>
    </main>

    <aside class="annotation-column annotation-form-column">
      <h2>标签（人工标注）</h2>
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
          <el-select v-else v-model="form[tag.taskTagId]" placeholder="请选择分类" class="wide-control">
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
    </aside>
  </section>
</template>
