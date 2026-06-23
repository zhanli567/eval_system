<script setup lang="ts">
import { Back, Plus, Promotion, Refresh, Delete } from '@element-plus/icons-vue'
import { useEvaluatorEditor } from '../modules/evaluator/composables/useEvaluatorEditor'

const {
  loading,
  saving,
  publishing,
  versions,
  activeVersionId,
  form,
  isEdit,
  canEdit,
  pageTitle,
  activeVersion,
  promptParams,
  modelOptions,
  modelLoading,
  handleModelVisibleChange,
  refreshEditor,
  selectVersion,
  submit,
  publishDraft,
  switchType,
  addParam,
  removeParam,
  backToList,
  formatTime
} = useEvaluatorEditor()
</script>

<template>
  <header class="topbar detail-topbar">
    <div>
      <el-button link type="primary" :icon="Back" class="back-link" @click="backToList">返回评估器列表</el-button>
      <p class="eyebrow">应用评测 / {{ isEdit ? '编辑评估器' : '创建评估器' }}</p>
      <h1>{{ pageTitle }}</h1>
      <span v-if="isEdit && activeVersion" class="meta">
        当前版本 {{ activeVersion.versionName }} · {{ activeVersion.draft ? '草稿可编辑' : '发布版本只读' }}
      </span>
    </div>
    <div class="top-actions">
      <el-button v-if="isEdit" :icon="Refresh" @click="refreshEditor">刷新</el-button>
      <el-button :loading="saving" type="primary" :disabled="isEdit && !canEdit" @click="submit">{{ isEdit ? '保存草稿' : '创建' }}</el-button>
      <el-button v-if="isEdit" type="success" :icon="Promotion" :loading="publishing" :disabled="!canEdit" @click="publishDraft">发布</el-button>
    </div>
  </header>

  <section class="evaluator-editor-shell" v-loading="loading">
    <aside v-if="isEdit" class="version-rail evaluator-version-rail">
      <div class="rail-title">
        <span>版本管理</span>
        <strong>{{ versions.length }}</strong>
      </div>
      <button
        v-for="version in versions"
        :key="version.id"
        class="version-item"
        :class="{ active: activeVersionId === version.id }"
        @click="selectVersion(version.id)"
      >
        <span>{{ version.versionName }}</span>
        <small>{{ formatTime(version.lastUpdatedDate) }}</small>
      </button>
    </aside>

    <main class="editor-main">
      <section class="editor-form-panel">
        <h2>配置</h2>

        <el-form label-position="top" class="evaluator-form">
          <el-form-item label="评估器名称" required>
            <el-input v-model="form.evaluatorName" maxlength="50" show-word-limit :disabled="!canEdit" placeholder="请输入" />
          </el-form-item>

          <el-form-item label="描述">
            <el-input
              v-model="form.description"
              type="textarea"
              maxlength="200"
              show-word-limit
              :disabled="!canEdit"
              placeholder="请输入描述"
            />
          </el-form-item>

          <el-form-item label="创建方式" required>
            <div class="method-grid">
              <button
                type="button"
                class="method-card"
                :class="{ active: form.evaluatorType === 'llm', disabled: isEdit && form.evaluatorType !== 'llm' }"
                :disabled="isEdit && form.evaluatorType !== 'llm'"
                @click="switchType('llm')"
              >
                <strong>LLM</strong>
                <span>通过 Prompt 设计规则，让大模型判断预期输出和实际输出的差异</span>
              </button>
              <button
                type="button"
                class="method-card"
                :class="{ active: form.evaluatorType === 'code', disabled: isEdit && form.evaluatorType !== 'code' }"
                :disabled="isEdit && form.evaluatorType !== 'code'"
                @click="switchType('code')"
              >
                <strong>Code</strong>
                <span>通过 Coding 设计规则，执行代码函数来对比预期输出和实际输出</span>
              </button>
            </div>
          </el-form-item>

          <template v-if="form.evaluatorType === 'llm'">
            <el-form-item label="选择模型" required>
              <el-select
                v-model="form.modelId"
                class="wide-control"
                :disabled="!canEdit"
                :loading="modelLoading"
                filterable
                placeholder="请选择模型"
                @visible-change="handleModelVisibleChange"
              >
                <el-option v-for="model in modelOptions" :key="model.value" :label="model.label" :value="model.value" />
              </el-select>
            </el-form-item>

            <el-form-item label="Prompt" required>
              <el-input
                v-model="form.prompt"
                type="textarea"
                :rows="16"
                maxlength="2000"
                show-word-limit
                :disabled="!canEdit"
              />
              <div class="prompt-param-row">
                <span>被引用的参数</span>
                <el-tag v-for="param in promptParams" :key="param.paramName" size="small">{{ param.paramName }}</el-tag>
                <span v-if="!promptParams.length" class="meta">暂无</span>
              </div>
            </el-form-item>

            <div class="dialog-subtitle required-title">
              <span>Prompt 参数配置</span>
            </div>
            <div class="param-editor-list">
              <div v-for="param in promptParams" :key="param.paramName" class="param-editor param-editor-llm">
                <el-input v-model="param.paramName" disabled placeholder="变量名" />
                <el-select v-model="param.dataType" :disabled="!canEdit" placeholder="数据类型">
                  <el-option label="string" value="string" />
                  <el-option label="number" value="number" />
                  <el-option label="boolean" value="boolean" />
                </el-select>
                <el-checkbox v-model="param.required" :disabled="!canEdit">必填</el-checkbox>
                <el-input v-model="param.description" :disabled="!canEdit" maxlength="200" placeholder="参数描述" />
              </div>
              <el-empty v-if="!promptParams.length" description="暂无参数" :image-size="72" />
            </div>
          </template>

          <template v-else>
            <div class="dialog-subtitle required-title">
              <span>代码入参设置</span>
              <el-button link type="primary" :icon="Plus" :disabled="!canEdit" @click="addParam">添加变量</el-button>
            </div>
            <div class="param-editor-list">
              <div v-for="(param, index) in form.params" :key="index" class="param-editor">
                <el-input v-model="param.paramName" :disabled="!canEdit" placeholder="变量名" />
                <el-select v-model="param.dataType" :disabled="!canEdit" placeholder="数据类型">
                  <el-option label="string" value="string" />
                  <el-option label="number" value="number" />
                  <el-option label="boolean" value="boolean" />
                </el-select>
                <el-checkbox v-model="param.required" :disabled="!canEdit">必填</el-checkbox>
                <el-input v-model="param.description" :disabled="!canEdit" maxlength="200" placeholder="参数描述" />
                <el-input v-model="param.defaultValue" :disabled="!canEdit" placeholder="默认值" />
                <el-button :icon="Delete" circle :disabled="!canEdit" @click="removeParam(index)" />
              </div>
            </div>

            <el-form-item label="执行函数" required>
              <el-input
                v-model="form.executeCode"
                type="textarea"
                :rows="14"
                maxlength="10000"
                show-word-limit
                :disabled="!canEdit"
              />
            </el-form-item>
          </template>

          <el-form-item label="评分范围" required>
            <div class="range-row">
              <el-input-number v-model="form.scoreMin" :disabled="!canEdit" />
              <span>-</span>
              <el-input-number v-model="form.scoreMax" :disabled="!canEdit" />
            </div>
          </el-form-item>

          <el-form-item label="通过阈值" required>
            <el-input-number v-model="form.passThreshold" class="wide-control" :disabled="!canEdit" />
          </el-form-item>
        </el-form>
      </section>
    </main>

  </section>
</template>
