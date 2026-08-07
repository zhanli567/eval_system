<script setup>
import { ChatDotRound, CopyDocument, Delete, Document, MagicStick, Plus, Promotion, Refresh, Tickets } from '@element-plus/icons-vue';
import { useEvaluatorEditor } from '../modules/evaluator/composables/useEvaluatorEditor';
const { loading, saving, publishing, versions, activeVersionId, form, isEdit, canEdit, pageTitle, activeVersion, promptParams, modelOptions, modelLoading, presetPickerVisible, presetCategories, presetEvaluators, presetPage, presetSize, presetTotal, presetKeyword, presetCategoryId, presetLoading, handleModelVisibleChange, refreshEditor, selectVersion, submit, publishDraft, removeVersion, switchType, addParam, removeParam, backToList, openPresetPicker, searchPreset, selectPresetCategory, changePresetPage, usePresetEvaluator, copyPrompt, clearPrompt, formatTime } = useEvaluatorEditor();
</script>

<template>
  <section class="evaluator-editor-shell" :class="{ 'is-create': !isEdit, 'is-edit': isEdit, 'task-create-shell': !isEdit }" v-loading="loading">
    <div class="embedded-page-title">
      <nav class="page-breadcrumb" aria-label="页面路径">
        <button type="button" class="page-breadcrumb-link" @click="backToList">评估器</button>
        <template v-if="pageTitle">
          <span class="page-breadcrumb-separator">/</span>
          <OverflowTooltip :content="pageTitle" tag="span" class="page-breadcrumb-current" />
        </template>
      </nav>
      <div class="embedded-title-actions">
        <template v-if="isEdit">
          <el-button class="toolbar-icon-button" :icon="Refresh" title="刷新" aria-label="刷新" @click="refreshEditor" />
          <template v-if="activeVersion?.draft">
            <el-button :loading="saving" type="primary" :disabled="!canEdit || form.evaluatorType === 'code'" @click="submit">保存草稿</el-button>
            <el-button type="success" :icon="Promotion" :loading="publishing" :disabled="!canEdit || form.evaluatorType === 'code'" @click="publishDraft">发布</el-button>
          </template>
          <el-button v-else-if="activeVersion" type="danger" plain :icon="Delete" @click="removeVersion(activeVersion)">删除版本</el-button>
        </template>
        <template v-else>
          <el-button @click="backToList">取消</el-button>
          <el-button type="primary" :loading="saving" :disabled="form.evaluatorType === 'code'" @click="submit">创建</el-button>
        </template>
      </div>
    </div>

    <aside v-if="isEdit" class="version-rail evaluator-version-rail">
      <div class="rail-title">
        <span>版本管理</span>
        <strong>{{ versions.filter((version) => !version.draft).length }}</strong>
      </div>
      <div
        v-for="version in versions"
        :key="version.id"
        class="version-item"
        :class="{ active: activeVersionId === version.id }"
        role="button"
        tabindex="0"
        @click="selectVersion(version.id)"
        @keyup.enter.self="selectVersion(version.id)"
        @keyup.space.self.prevent="selectVersion(version.id)"
      >
        <span class="version-item-title">{{ version.versionName }}</span>
        <span class="version-item-meta">
          <small>{{ version.createdByName || '-' }}</small>
          <small>{{ formatTime(version.createdDate) }}</small>
        </span>
      </div>
    </aside>

    <main :class="isEdit ? 'editor-main' : 'task-create-main evaluator-create-main'">
      <section :class="isEdit ? 'editor-form-panel' : 'evaluator-create-panel'">
        <h2 v-if="isEdit">配置</h2>

        <el-form label-position="top" class="evaluator-form">
          <section :class="{ 'task-create-section': !isEdit }">
            <div v-if="!isEdit" class="section-index">1</div>
            <div class="section-body">
              <h2 v-if="!isEdit">基本信息</h2>
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
            </div>
          </section>

          <section :class="{ 'task-create-section': !isEdit }">
            <div v-if="!isEdit" class="section-index">2</div>
            <div class="section-body">
              <h2 v-if="!isEdit">评估器信息</h2>
              <el-form-item label="创建方式" required>
                <div class="method-grid">
                  <button
                    type="button"
                    class="method-card"
                    :class="{ active: form.evaluatorType === 'llm', disabled: !canEdit && form.evaluatorType !== 'llm' }"
                    :disabled="!canEdit && form.evaluatorType !== 'llm'"
                    @click="switchType('llm')"
                  >
                    <span class="method-card-icon">
                      <el-icon><ChatDotRound /></el-icon>
                    </span>
                    <strong>LLM</strong>
                    <span>通过 Prompt 设计规则，让大模型判断预期输出和实际输出的差异</span>
                    <i class="method-card-check" />
                  </button>
                  <button
                    type="button"
                    class="method-card"
                    :class="{ active: form.evaluatorType === 'code', disabled: true }"
                    disabled
                    @click="switchType('code')"
                  >
                    <span class="method-card-icon">
                      <el-icon><Document /></el-icon>
                    </span>
                    <strong>Code</strong>
                    <span>通过 Coding 设计规则，执行代码函数来对比预期输出和实际输出</span>
                    <i class="method-card-check" />
                  </button>
                  <button v-if="!isEdit" type="button" class="method-card disabled" disabled>
                    <span class="method-card-icon">
                      <el-icon><Tickets /></el-icon>
                    </span>
                    <strong>基于评测任务</strong>
                    <span>通过历史评测任务的标注结果，抽象并总结为新的 LLM 评估器</span>
                    <i class="method-card-check" />
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
                    clearable
                    placeholder="请选择模型"
                    @visible-change="handleModelVisibleChange"
                  >
                    <el-option v-for="model in modelOptions" :key="model.value" :label="model.label" :value="model.value" />
                  </el-select>
                </el-form-item>

                <el-form-item required>
                  <template #label>
                    <div class="prompt-label-row">
                      <span>Prompt</span>
                      <div v-if="!isEdit && canEdit" class="prompt-tool-actions">
                        <el-button link type="primary" :icon="MagicStick" disabled>AI优化</el-button>
                        <el-button link type="primary" :icon="Tickets" @click="openPresetPicker">选择预置评估器</el-button>
                        <el-button link type="primary" :icon="CopyDocument" @click="copyPrompt">复制</el-button>
                        <el-button link type="primary" :icon="Delete" @click="clearPrompt">清空</el-button>
                      </div>
                    </div>
                  </template>
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
                    <el-select v-model="param.dataType" clearable :disabled="!canEdit" placeholder="数据类型">
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
                    <el-select v-model="param.dataType" clearable :disabled="!canEdit" placeholder="数据类型">
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

                <el-form-item label="Code" required>
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
            </div>
          </section>

          <section :class="{ 'task-create-section': !isEdit }">
            <div v-if="!isEdit" class="section-index">3</div>
            <div class="section-body">
              <h2 v-if="!isEdit">评分信息</h2>
              <el-form-item label="评分范围" required>
                <div class="range-row">
                  <el-input-number v-model="form.scoreMin" controls-position="right" class="quiet-input-number" :disabled="!canEdit" />
                  <span>-</span>
                  <el-input-number v-model="form.scoreMax" controls-position="right" class="quiet-input-number" :disabled="!canEdit" />
                </div>
              </el-form-item>

              <el-form-item label="通过阈值" required>
                <el-input-number v-model="form.passThreshold" controls-position="right" class="wide-control quiet-input-number" :disabled="!canEdit" />
              </el-form-item>
            </div>
          </section>
        </el-form>
      </section>
    </main>

    <el-dialog v-model="presetPickerVisible" title="选择预置评估器" class="evaluator-picker-dialog fixed-dialog" style="--fixed-dialog-width: min(980px, 88vw); --fixed-dialog-height: min(680px, 84vh)" :close-on-click-modal="true">
      <div class="preset-layout picker-layout">
        <aside class="preset-category-rail">
          <span class="rail-caption">预置评估器分类</span>
          <button
            v-for="category in presetCategories"
            :key="category.id || 'preset-all'"
            type="button"
            :class="{ active: presetCategoryId === category.id }"
            @click="selectPresetCategory(category.id)"
          >
            {{ category.categoryName }}
          </button>
        </aside>
        <div class="preset-content">
          <div class="picker-head">
            <div>
              <h2>全部分类预置评估器</h2>
            </div>
            <div class="picker-actions">
              <el-input v-model="presetKeyword" clearable placeholder="请输入预置评估器名称" maxlength="50" show-word-limit class="search-input" @keyup.enter="searchPreset" @clear="searchPreset" />
            </div>
          </div>
          <div v-loading="presetLoading" class="preset-grid picker-grid">
            <article
              v-for="preset in presetEvaluators"
              :key="preset.id"
              class="preset-card"
              :class="{ disabled: preset.evaluatorType === 'code' }"
            >
              <div class="preset-card-head">
                <span class="preset-card-icon">
                  <el-icon><Tickets /></el-icon>
                </span>
                <strong>{{ preset.evaluatorName }}</strong>
                <span class="preset-type-pill">{{ preset.evaluatorType === 'code' ? 'Code' : 'LLM' }}</span>
              </div>
              <p>{{ preset.description || '暂无描述' }}</p>
              <div class="preset-card-actions">
                <el-button link type="primary" :disabled="preset.evaluatorType === 'code'" @click.stop="usePresetEvaluator(preset.id)">使用此评估器</el-button>
              </div>
            </article>
            <el-empty v-if="!presetEvaluators.length && !presetLoading" description="暂无预置评估器" :image-size="80" />
          </div>
          <div class="pager-row">
            <el-pagination
              v-model:current-page="presetPage"
              v-model:page-size="presetSize"
              small
              :page-sizes="[9, 18, 27]"
              layout="total, sizes, prev, pager, next"
              :total="presetTotal"
              @current-change="changePresetPage"
              @size-change="changePresetPage"
            />
          </div>
        </div>
      </div>
    </el-dialog>
  </section>
</template>
