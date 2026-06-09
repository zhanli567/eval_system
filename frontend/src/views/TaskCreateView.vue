<script setup lang="ts">
import { Back, Delete, Plus } from '@element-plus/icons-vue'
import { useTaskCreate } from '../modules/task/composables/useTaskCreate'

const {
  loading,
  saving,
  datasets,
  publishedVersions,
  fields,
  tags,
  selectedTagIds,
  customEvaluators,
  categoryOptions,
  evaluatorBlocks,
  form,
  changePresetCategory,
  changeEvaluatorSource,
  selectEvaluator,
  selectCustomVersion,
  addEvaluator,
  removeEvaluator,
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
    <div class="top-actions">
      <el-button @click="backToList">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">创建</el-button>
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
              <el-radio-group v-model="form.appType">
                <el-radio-button label="none">不关联应用</el-radio-button>
                <el-radio-button label="agent" disabled>智能体（待接入）</el-radio-button>
              </el-radio-group>
              <span class="hint">当前先实现任务与数据关系，智能体应用接入后将开放字段映射。</span>
            </el-form-item>
          </el-form>
        </div>
      </section>

      <section class="task-create-section">
        <div class="section-index">2</div>
        <div class="section-body">
          <h2>字段映射</h2>
          <div v-if="form.appType === 'agent'" class="mapping-placeholder">
            智能体应用接入后，在这里将智能体输入变量映射到评测集字段。
          </div>
          <div v-else class="mapping-placeholder">
            当前任务不关联应用，无需配置应用字段映射。
          </div>
        </div>
      </section>

      <section class="task-create-section">
        <div class="section-index">3</div>
        <div class="section-body">
          <div class="section-head">
            <h2>评估器</h2>
            <el-button link type="primary" :icon="Plus" @click="addEvaluator">添加评估器</el-button>
          </div>

          <article v-for="(block, index) in evaluatorBlocks" :key="block.key" class="evaluator-config-card" v-loading="block.loading">
            <div class="evaluator-config-head">
              <strong>{{ block.evaluatorName || `评估器 ${index + 1}` }}</strong>
              <el-button :icon="Delete" circle :disabled="evaluatorBlocks.length <= 1" @click="removeEvaluator(index)" />
            </div>

            <div class="evaluator-config-grid">
              <el-form-item label="评估器类型 *">
                <el-radio-group v-model="block.evaluatorSource" @change="changeEvaluatorSource(block)">
                  <el-radio-button label="custom">自定义评估器</el-radio-button>
                  <el-radio-button label="preset">预置评估器</el-radio-button>
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
                <el-input
                  v-else
                  v-model="block.paramMappings[paramKey(param)].appOutputName"
                  placeholder="应用输出字段名，单一输出可为空"
                />
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
        <div class="section-index">4</div>
        <div class="section-body">
          <h2>标签</h2>
          <el-select v-model="selectedTagIds" multiple filterable collapse-tags collapse-tags-tooltip placeholder="请选择标签，最多5个" class="wide-control">
            <el-option v-for="tag in tags" :key="tag.id" :label="`${tag.tagName} · ${tagTypeLabel(tag.tagType)}`" :value="tag.id" />
          </el-select>
          <div class="selected-tag-list">
            <el-tag v-for="tagId in selectedTagIds" :key="tagId" effect="plain">
              {{ tags.find((tag) => tag.id === tagId)?.tagName || tagId }}
            </el-tag>
          </div>
        </div>
      </section>
    </main>
  </section>
</template>
