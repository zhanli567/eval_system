<script setup>
import { ref } from 'vue';
import { ArrowDown, ArrowRight, CopyDocument, Delete, Plus, Refresh } from '@element-plus/icons-vue';
import TagCreateDialog from '../components/TagCreateDialog.vue';
import TaskTagDrawer from '../components/TaskTagDrawer.vue';
import { useTaskCreate } from '../modules/task/composables/useTaskCreate';
import { formatPromptBlock } from '../utils/textBlocks';
const {
    loading,
    saving,
    tagDrawerVisible,
    tagKeyword,
    tagTypeFilter,
    tagLoading,
    tagPage,
    tagSize,
    tagTotal,
    datasets,
    publishedVersions,
    fields,
    tags,
    selectedTagIds,
    selectedTags,
    tagTypeOptions,
    customEvaluators,
    categoryOptions,
    evaluatorBlocks,
    agents,
    models,
    agentDetailLoading,
    agentVersionLoading,
    modelLoading,
    agentVersions,
    agentChildAgents,
    agentInputs,
    agentOutputs,
    appFieldMappings,
    form,
    handleDatasetVisible,
    handleAgentVisible,
    handleCustomEvaluatorVisible,
    handlePresetCategoryVisible,
    handlePresetEvaluatorVisible,
    handleModelVisible,
    changePresetCategory,
    changeEvaluatorSource,
    selectEvaluator,
    selectCustomVersion,
    addEvaluator,
    removeEvaluator,
    resetEvaluator,
    resetParamMapping,
    resetDatasetSelection,
    resetAgentSelection,
    resetAppFieldMapping,
    copyBlockPrompt,
    loadTags,
    openTagDrawer,
    searchTags,
    changeTagSize,
    addTag,
    removeTag,
    submit,
    paramKey,
    fieldTypeLabel,
    agentOutputLabel,
    tagTypeLabel,
    backToList
} = useTaskCreate();
const tagCreateVisible = ref(false);

async function refreshTagsAfterCreate() {
    await loadTags();
}
</script>

<template>
  <section class="task-create-shell" v-loading="loading">
    <div class="embedded-page-title">
      <nav class="page-breadcrumb" aria-label="页面路径">
        <button type="button" class="page-breadcrumb-link" @click="backToList">评测任务</button>
        <span class="page-breadcrumb-separator">/</span>
        <span class="page-breadcrumb-current">创建评测任务</span>
      </nav>
      <div class="embedded-title-actions">
        <el-button @click="backToList">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">创建</el-button>
      </div>
    </div>

    <main class="task-create-main">
      <section class="task-create-section">
        <div class="section-index">1</div>
        <div class="section-body">
          <h2>基础信息</h2>
          <el-form label-position="top" class="task-create-form">
            <el-form-item>
              <template #label>任务名称 <span class="required-mark">*</span></template>
              <el-input v-model="form.taskName" maxlength="50" show-word-limit placeholder="请输入任务名称" />
            </el-form-item>
            <el-form-item label="描述">
              <el-input v-model="form.description" type="textarea" maxlength="200" show-word-limit :autosize="{ minRows: 4, maxRows: 6 }" />
            </el-form-item>
            <el-form-item>
              <template #label>选择评测集及版本 <span class="required-mark">*</span></template>
              <div class="inline-controls select-reset-group">
                <el-select v-model="form.datasetId" placeholder="请选择评测集" filterable @visible-change="handleDatasetVisible">
                  <el-option v-for="dataset in datasets" :key="dataset.id" :label="dataset.name" :value="dataset.id" />
                </el-select>
                <el-select v-model="form.datasetVersionId" placeholder="请选择发布版本" :disabled="!form.datasetId">
                  <el-option
                    v-for="version in publishedVersions"
                    :key="version.id"
                    :label="`${version.versionName} · ${version.itemCount}条`"
                    :value="version.id"
                  />
                </el-select>
                <el-button class="toolbar-icon-button" :icon="Refresh" title="重置" aria-label="重置评测集" @click="resetDatasetSelection" />
              </div>
              <span v-if="form.datasetId && !publishedVersions.length" class="hint">该评测集暂无发布版本，请先发布评测集。</span>
            </el-form-item>
            <el-form-item>
              <template #label>选择应用 <span class="required-mark">*</span></template>
              <div class="app-picker">
                <el-radio-group v-model="form.appType" class="plain-radio-group">
                  <el-radio label="none">不关联应用</el-radio>
                  <el-radio label="agent">智能体</el-radio>
                </el-radio-group>
                <div v-if="form.appType === 'agent'" class="app-select-grid select-reset-group">
                  <el-select v-model="form.appId" placeholder="请选择智能体" filterable @visible-change="handleAgentVisible">
                    <el-option v-for="agent in agents" :key="agent.id" :label="agent.agentName" :value="agent.id" />
                  </el-select>
                  <el-select v-model="form.appVersionId" placeholder="请选择智能体版本" :disabled="!form.appId" :loading="agentVersionLoading">
                    <el-option v-for="version in agentVersions" :key="version.id" :label="version.versionName" :value="version.id" />
                  </el-select>
                  <el-select v-model="form.appAgentAlias" placeholder="选择子智能体（可选）" :disabled="!form.appId" :loading="agentDetailLoading">
                    <el-option label="超级智能体" value="" />
                    <el-option
                      v-for="child in agentChildAgents"
                      :key="child.agentAlias"
                      :label="child.agentName ? `${child.agentName} · ${child.agentAlias}` : child.agentAlias"
                      :value="child.agentAlias"
                    />
                  </el-select>
                  <el-button class="toolbar-icon-button" :icon="Refresh" title="重置" aria-label="重置应用" @click="resetAgentSelection" />
                </div>
              </div>
            </el-form-item>
          </el-form>
        </div>
      </section>

      <section v-if="form.appType === 'agent'" class="task-create-section">
        <div class="section-index">2</div>
        <div class="section-body">
          <h2>字段映射</h2>
          <div class="app-mapping-panel">
            <div class="param-mapping-list app-field-mapping-list">
              <div v-for="input in agentInputs" :key="input.id" class="param-mapping-row app-field-mapping-row">
                <div class="param-cell plain-param-cell">
                  <span>{{ input.fieldName }}</span>
                </div>
                <span class="mapping-arrow">→</span>
                <span class="mapping-source-label">评测集字段</span>
                <el-select v-model="appFieldMappings[input.id]" filterable placeholder="请选择评测集字段" :disabled="!form.datasetVersionId">
                  <el-option v-for="field in fields" :key="field.id" :label="`${field.fieldName} · ${fieldTypeLabel(field.fieldType)}`" :value="field.id" />
                </el-select>
                <el-button class="toolbar-icon-button" :icon="Refresh" title="重置" aria-label="重置字段映射" @click="resetAppFieldMapping(input.id)" />
              </div>
            </div>
          </div>
        </div>
      </section>

      <section class="task-create-section">
        <div class="section-index">{{ form.appType === 'agent' ? 3 : 2 }}</div>
        <div class="section-body">
          <div class="section-head">
            <h2>评估器</h2>
            <el-button link type="primary" :icon="Plus" :disabled="evaluatorBlocks.length >= 5" @click="addEvaluator">添加评估器</el-button>
          </div>

          <article v-for="(block, index) in evaluatorBlocks" :key="block.key" class="evaluator-config-card" v-loading="block.loading">
            <div class="evaluator-config-head">
              <strong>{{ block.evaluatorName || `评估器 ${index + 1}` }}</strong>
              <div class="evaluator-card-actions">
                <el-button class="bare-icon-button" :icon="Refresh" text title="重置" aria-label="重置评估器" @click="resetEvaluator(block)" />
                <el-button class="bare-icon-button" :icon="Delete" text title="删除" aria-label="删除评估器" @click="removeEvaluator(index)" />
              </div>
            </div>

            <div class="evaluator-config-grid">
              <el-form-item class="evaluator-config-full">
                <template #label>评估器类型 <span class="required-mark">*</span></template>
                <el-radio-group v-model="block.evaluatorSource" class="plain-radio-group" @change="changeEvaluatorSource(block)">
                  <el-radio label="custom">自定义评估器</el-radio>
                  <el-radio label="preset">预置评估器</el-radio>
                </el-radio-group>
              </el-form-item>

              <template v-if="block.evaluatorSource === 'preset'">
                <el-form-item label="分类">
                  <el-select v-model="block.presetCategoryId" @visible-change="handlePresetCategoryVisible" @change="changePresetCategory(block)">
                    <el-option v-for="category in categoryOptions" :key="category.id || 'all'" :label="category.categoryName" :value="category.id" />
                  </el-select>
                </el-form-item>
                <el-form-item>
                  <template #label>选择评估器 <span class="required-mark">*</span></template>
                  <el-select
                    v-model="block.evaluatorId"
                    filterable
                    placeholder="请选择预置评估器"
                    @visible-change="handlePresetEvaluatorVisible(block, $event)"
                    @change="selectEvaluator(block)"
                  >
                    <el-option v-for="item in block.presetOptions" :key="item.id" :label="item.evaluatorName" :value="item.id" :disabled="item.evaluatorType === 'code'" />
                  </el-select>
                </el-form-item>
                <el-form-item v-if="block.evaluatorType === 'llm'" class="evaluator-config-full">
                  <template #label>选择模型 <span class="required-mark">*</span></template>
                  <el-select
                    v-model="block.modelId"
                    filterable
                    :loading="modelLoading"
                    placeholder="请选择评估模型"
                    @visible-change="handleModelVisible"
                  >
                    <el-option
                      v-for="model in models"
                      :key="model.modelId"
                      :label="model.name || model.modelName || model.modelId"
                      :value="model.modelId"
                    />
                  </el-select>
                </el-form-item>
              </template>

              <template v-else>
                <el-form-item>
                  <template #label>选择评估器 <span class="required-mark">*</span></template>
                  <el-select
                    v-model="block.evaluatorId"
                    filterable
                    placeholder="请选择自定义评估器"
                    @visible-change="handleCustomEvaluatorVisible"
                    @change="selectEvaluator(block)"
                  >
                    <el-option v-for="item in customEvaluators" :key="item.id" :label="item.evaluatorName" :value="item.id" :disabled="item.evaluatorType === 'code'" />
                  </el-select>
                </el-form-item>
                <el-form-item>
                  <template #label>选择版本 <span class="required-mark">*</span></template>
                  <el-select v-model="block.evaluatorVersionId" placeholder="请选择版本" :disabled="!block.evaluatorId" @change="selectCustomVersion(block)">
                    <el-option v-for="version in block.versions" :key="version.id" :label="version.versionName" :value="version.id" />
                  </el-select>
                </el-form-item>
              </template>
            </div>

            <el-button v-if="block.evaluatorName" class="evaluator-detail-toggle" link type="primary" @click="block.detailExpanded = !block.detailExpanded">
              <el-icon>
                <ArrowDown v-if="block.detailExpanded" />
                <ArrowRight v-else />
              </el-icon>
              <span>评估器详情</span>
            </el-button>

            <div v-if="block.params.length" class="param-mapping-list">
              <h3>字段映射</h3>
              <div v-for="param in block.params" :key="paramKey(param)" class="param-mapping-row">
                <div class="param-cell plain-param-cell">
                  <span>{{ param.paramName }}<span v-if="param.required" class="required-mark">*</span></span>
                </div>
                <span class="mapping-arrow">→</span>
                <el-select v-model="block.paramMappings[paramKey(param)].sourceType" class="mapping-source">
                  <el-option label="评测集" value="dataset_field" />
                  <el-option label="应用输出" value="app_output" :disabled="form.appType !== 'agent'" />
                </el-select>
                <el-select
                  v-if="block.paramMappings[paramKey(param)].sourceType === 'dataset_field'"
                  v-model="block.paramMappings[paramKey(param)].datasetFieldId"
                  filterable
                  placeholder="请选择评测集字段"
                >
                  <el-option v-for="field in fields" :key="field.id" :label="`${field.fieldName} · ${fieldTypeLabel(field.fieldType)}`" :value="field.id" />
                </el-select>
                <el-select
                  v-else
                  v-model="block.paramMappings[paramKey(param)].appOutputName"
                  placeholder="请选择应用输出字段"
                  :disabled="form.appType !== 'agent'"
                >
                  <el-option
                    v-for="output in agentOutputs"
                    :key="output.id"
                    :label="agentOutputLabel(output)"
                    :value="output.fieldName"
                  />
                </el-select>
                <el-button class="toolbar-icon-button" :icon="Refresh" title="重置" aria-label="重置参数映射" @click="resetParamMapping(block, param)" />
              </div>
            </div>

            <div v-if="block.detailExpanded" class="evaluator-inline-detail">
              <p>{{ block.description || '暂无描述' }}</p>
              <div class="score-summary">
                <span>类型：{{ block.evaluatorType || '-' }}</span>
                <span>评分范围：{{ block.scoreMin ?? '-' }} - {{ block.scoreMax ?? '-' }}</span>
                <span>通过阈值：{{ block.passThreshold ?? '-' }}</span>
              </div>
              <template v-if="block.evaluatorType === 'llm'">
                <div class="inline-detail-head">
                  <span>Prompt</span>
                  <el-button link type="primary" :icon="CopyDocument" @click="copyBlockPrompt(block)">复制</el-button>
                </div>
                <pre class="code-block">{{ formatPromptBlock(block.prompt) }}</pre>
              </template>
              <pre v-else class="code-block">{{ block.executeCode }}</pre>
            </div>
          </article>
        </div>
      </section>

      <section class="task-create-section">
        <div class="section-index">{{ form.appType === 'agent' ? 4 : 3 }}</div>
        <div class="section-body">
          <div class="section-head">
            <h2>标签</h2>
            <el-button link type="primary" :icon="Plus" @click="openTagDrawer">添加标签</el-button>
          </div>
          <div class="selected-tag-list">
            <article v-for="tag in selectedTags" :key="tag.id" class="tag-picker-card selected-tag-card">
              <div class="tag-picker-card-main">
                <div class="tag-title-row">
                  <strong>{{ tag.tagName }}</strong>
                  <el-tag size="small" effect="plain">{{ tagTypeLabel(tag.tagType) }}</el-tag>
                </div>
                <p>{{ tag.description || '暂无描述' }}</p>
              </div>
              <el-button class="bare-icon-button" :icon="Delete" text title="移除" aria-label="移除标签" @click="removeTag(tag.id)" />
            </article>
          </div>
        </div>
      </section>
    </main>

    <TaskTagDrawer
      v-model="tagDrawerVisible"
      v-model:keyword="tagKeyword"
      v-model:tag-type="tagTypeFilter"
      v-model:page="tagPage"
      v-model:size="tagSize"
      title="添加标签"
      :tags="tags"
      :selected-tag-ids="selectedTagIds"
      :tag-type-options="tagTypeOptions"
      :loading="tagLoading"
      :total="tagTotal"
      @refresh="loadTags"
      @search="searchTags"
      @page-change="loadTags"
      @size-change="changeTagSize"
      @create="tagCreateVisible = true"
      @add="addTag"
      @remove="(tag) => removeTag(tag.id)"
    />
    <TagCreateDialog v-model="tagCreateVisible" @created="refreshTagsAfterCreate" />
  </section>
</template>
