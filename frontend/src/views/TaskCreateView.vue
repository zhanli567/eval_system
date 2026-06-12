<script setup lang="ts">
import { Back, Delete, Plus } from '@element-plus/icons-vue'
import { useTaskCreate } from '../modules/task/composables/useTaskCreate'

const {
  loading,
  saving,
  tagDrawerVisible,
  tagKeyword,
  tagTypeFilter,
  datasets,
  publishedVersions,
  fields,
  selectedTags,
  filteredTags,
  tagTypeOptions,
  customEvaluators,
  categoryOptions,
  evaluatorBlocks,
  agents,
  agentVersions,
  agentInputs,
  agentOutputs,
  appFieldMappings,
  form,
  changePresetCategory,
  changeEvaluatorSource,
  selectEvaluator,
  selectCustomVersion,
  addEvaluator,
  removeEvaluator,
  openTagDrawer,
  addTag,
  removeTag,
  isTagSelected,
  submit,
  paramKey,
  fieldTypeLabel,
  tagTypeLabel,
  backToList
} = useTaskCreate()
</script>

<template>
  <header class="topbar detail-topbar">
    <div>
      <el-button link type="primary" :icon="Back" class="back-link" @click="backToList">返回评测任务列表</el-button>
      <p class="eyebrow">运行评测 / 创建评测任务</p>
      <h1>创建评测任务</h1>
    </div>
  </header>

  <section class="task-create-shell" v-loading="loading">
    <main class="task-create-main">
      <section class="task-create-section">
        <div class="section-index">1</div>
        <div class="section-body">
          <h2>基础信息</h2>
          <el-form label-position="top" class="task-create-form">
            <el-form-item label="任务名称 *">
              <el-input v-model="form.taskName" maxlength="50" show-word-limit placeholder="请输入任务名称" />
            </el-form-item>
            <el-form-item label="描述">
              <el-input v-model="form.description" type="textarea" maxlength="200" show-word-limit :autosize="{ minRows: 4, maxRows: 6 }" />
            </el-form-item>
            <el-form-item label="选择评测集及版本 *">
              <div class="inline-controls">
                <el-select v-model="form.datasetId" placeholder="请选择评测集" filterable>
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
              </div>
              <span v-if="form.datasetId && !publishedVersions.length" class="hint">该评测集暂无发布版本，请先发布评测集。</span>
            </el-form-item>
            <el-form-item label="选择应用">
              <div class="app-picker">
                <el-radio-group v-model="form.appType" class="plain-radio-group">
                  <el-radio label="none">不关联应用</el-radio>
                  <el-radio label="agent">智能体</el-radio>
                </el-radio-group>
                <div v-if="form.appType === 'agent'" class="app-select-grid">
                  <el-select v-model="form.appId" placeholder="请选择智能体" filterable>
                    <el-option v-for="agent in agents" :key="agent.id" :label="agent.agentName" :value="agent.id" />
                  </el-select>
                  <el-select v-model="form.appVersionId" placeholder="请选择智能体版本" :disabled="!form.appId">
                    <el-option v-for="version in agentVersions" :key="version.id" :label="version.versionName" :value="version.id" />
                  </el-select>
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
                <div class="param-cell">
                  <strong>{{ input.fieldName }}</strong>
                  <el-tag size="small" effect="plain">{{ input.fieldType || 'string' }}</el-tag>
                </div>
                <span class="mapping-arrow">→</span>
                <span class="mapping-source-label">评测集字段</span>
                <el-select v-model="appFieldMappings[input.id]" filterable placeholder="请选择评测集字段" :disabled="!form.datasetVersionId">
                  <el-option v-for="field in fields" :key="field.id" :label="`${field.fieldName} · ${fieldTypeLabel(field.fieldType)}`" :value="field.id" />
                </el-select>
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
              <el-button :icon="Delete" circle @click="removeEvaluator(index)" />
            </div>

            <div class="evaluator-config-grid">
              <el-form-item label="评估器类型 *">
                <el-radio-group v-model="block.evaluatorSource" class="plain-radio-group" @change="changeEvaluatorSource(block)">
                  <el-radio label="custom">自定义评估器</el-radio>
                  <el-radio label="preset">预置评估器</el-radio>
                </el-radio-group>
              </el-form-item>

              <template v-if="block.evaluatorSource === 'preset'">
                <el-form-item label="分类">
                  <el-select v-model="block.presetCategoryId" @change="changePresetCategory(block)">
                    <el-option v-for="category in categoryOptions" :key="category.id || 'all'" :label="category.categoryName" :value="category.id" />
                  </el-select>
                </el-form-item>
                <el-form-item label="选择评估器 *">
                  <el-select v-model="block.evaluatorId" filterable placeholder="请选择预置评估器" @change="selectEvaluator(block)">
                    <el-option v-for="item in block.presetOptions" :key="item.id" :label="item.evaluatorName" :value="item.id" />
                  </el-select>
                </el-form-item>
              </template>

              <template v-else>
                <el-form-item label="选择评估器 *">
                  <el-select v-model="block.evaluatorId" filterable placeholder="请选择自定义评估器" @change="selectEvaluator(block)">
                    <el-option v-for="item in customEvaluators" :key="item.id" :label="item.evaluatorName" :value="item.id" />
                  </el-select>
                </el-form-item>
                <el-form-item label="选择版本 *">
                  <el-select v-model="block.evaluatorVersionId" placeholder="请选择版本" :disabled="!block.evaluatorId" @change="selectCustomVersion(block)">
                    <el-option v-for="version in block.versions" :key="version.id" :label="version.versionName" :value="version.id" />
                  </el-select>
                </el-form-item>
              </template>
            </div>

            <div v-if="block.params.length" class="param-mapping-list">
              <h3>字段映射</h3>
              <div v-for="param in block.params" :key="paramKey(param)" class="param-mapping-row">
                <div class="param-cell">
                  <strong>{{ param.paramName }}</strong>
                  <el-tag v-if="param.required" size="small" type="danger" effect="plain">必填</el-tag>
                  <span>{{ param.dataType }}</span>
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
                    :label="`${output.fieldName} · ${output.description || fieldTypeLabel(output.fieldType)}`"
                    :value="output.fieldName"
                  />
                </el-select>
              </div>
            </div>

            <el-button v-if="block.evaluatorName" link type="primary" @click="block.detailExpanded = !block.detailExpanded">
              {{ block.detailExpanded ? '收起评估器详情' : '查看评估器详情' }}
            </el-button>
            <div v-if="block.detailExpanded" class="evaluator-inline-detail">
              <p>{{ block.description || '暂无描述' }}</p>
              <div class="score-summary">
                <span>类型：{{ block.evaluatorType || '-' }}</span>
                <span>评分范围：{{ block.scoreMin ?? '-' }} - {{ block.scoreMax ?? '-' }}</span>
                <span>通过阈值：{{ block.passThreshold ?? '-' }}</span>
              </div>
              <pre v-if="block.evaluatorType === 'llm'" class="code-block">{{ block.prompt }}</pre>
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
              <el-button :icon="Delete" circle @click="removeTag(tag.id)" />
            </article>
            <span v-if="!selectedTags.length" class="selected-empty-text">暂无标签</span>
          </div>
        </div>
      </section>
    </main>

    <div class="task-create-bottom-bar">
      <el-button @click="backToList">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">创建</el-button>
    </div>

    <el-drawer v-model="tagDrawerVisible" title="添加标签" direction="rtl" size="460px" class="tag-picker-drawer">
      <div class="tag-picker-toolbar">
        <el-input v-model="tagKeyword" clearable placeholder="搜索标签名或描述" />
        <el-select v-model="tagTypeFilter" clearable placeholder="全部类别">
          <el-option v-for="type in tagTypeOptions" :key="type.value" :label="type.label" :value="type.value" />
        </el-select>
      </div>

      <div class="tag-picker-list">
        <article v-for="tag in filteredTags" :key="tag.id" class="tag-picker-card">
          <div class="tag-picker-card-main">
            <div class="tag-title-row">
              <strong>{{ tag.tagName }}</strong>
              <el-tag size="small" effect="plain">{{ tagTypeLabel(tag.tagType) }}</el-tag>
            </div>
            <p>{{ tag.description || '暂无描述' }}</p>
          </div>
          <el-button type="primary" plain :disabled="isTagSelected(tag.id)" @click="addTag(tag)">
            {{ isTagSelected(tag.id) ? '已添加' : '添加' }}
          </el-button>
        </article>
        <el-empty v-if="!filteredTags.length" description="暂无匹配标签" :image-size="80" />
      </div>
    </el-drawer>
  </section>
</template>
