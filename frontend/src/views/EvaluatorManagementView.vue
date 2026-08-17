<script setup>
import { DataAnalysis, Plus, Refresh, Search } from '@element-plus/icons-vue';
import ResourceDescriptionDialog from '../components/ResourceDescriptionDialog.vue';
import { useEvaluatorManagement } from '../modules/evaluator/composables/useEvaluatorManagement';
import { formatPromptBlock } from '../utils/textBlocks';
const { activeTab, customLoading, customEvaluators, customTotal, customPage, customSize, customKeyword, customType, customSortBy, customSortOrder, categoryOptions, activeCategoryId, presetLoading, presetEvaluators, presetTotal, presetPage, presetSize, presetKeyword, pickerVisible, pickerCategoryId, pickerKeyword, pickerPage, pickerSize, pickerTotal, pickerLoading, pickerPresets, detailVisible, detailLoading, selectedPreset, descriptionDialogVisible, descriptionSaving, descriptionForm, loadCustomEvaluators, searchCustom, changeCustomSize, toggleCustomSort, loadPresetEvaluators, searchPreset, changePresetSize, selectPresetCategory, openPicker, loadPickerPresets, searchPicker, changePickerSize, selectPickerCategory, viewPreset, createCustom, createFromPreset, editEvaluator, removeEvaluator, isOpeningPreset, isDeletingEvaluator, openDescriptionDialog, submitDescription, typeLabel, formatTime } = useEvaluatorManagement();
</script>

<template>
  <section class="evaluator-panel fill-workspace">
    <div class="evaluator-management-head">
      <div class="evaluator-tabs">
        <button :class="{ active: activeTab === 'custom' }" @click="activeTab = 'custom'">自定义</button>
        <button :class="{ active: activeTab === 'preset' }" @click="activeTab = 'preset'">预置</button>
      </div>

      <div v-if="activeTab === 'custom'" class="panel-toolbar table-toolbar evaluator-head-toolbar">
        <el-select v-model="customType" clearable placeholder="全部类型" class="field-select evaluator-type-select" @change="searchCustom">
          <el-option label="LLM" value="llm" />
          <el-option label="Code" value="code" disabled />
        </el-select>
        <el-input
          v-model="customKeyword"
          clearable
          placeholder="请输入名称"
          class="search-input"
          @keyup.enter="searchCustom"
          @clear="searchCustom"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button class="search-icon-button" :icon="Search" title="搜索" aria-label="搜索" @click="searchCustom" />
        <el-button class="toolbar-icon-button" :icon="Refresh" title="刷新" aria-label="刷新" @click="loadCustomEvaluators" />
        <el-button type="primary" :icon="Plus" @click="openPicker">创建评估器</el-button>
      </div>

      <div v-else class="panel-toolbar table-toolbar evaluator-head-toolbar">
        <el-input
          v-model="presetKeyword"
          clearable
          placeholder="请输入名称"
          class="search-input"
          @keyup.enter="searchPreset"
          @clear="searchPreset"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button class="search-icon-button" :icon="Search" title="搜索" aria-label="搜索" @click="searchPreset" />
        <el-button class="toolbar-icon-button" :icon="Refresh" title="刷新" aria-label="刷新" @click="loadPresetEvaluators" />
        <el-button type="primary" :icon="Plus" @click="openPicker">创建评估器</el-button>
      </div>
    </div>

    <template v-if="activeTab === 'custom'">
      <el-table
        v-loading="customLoading"
        :data="customEvaluators"
        row-key="id"
        border
        height="100%"
        tooltip-effect="light"
        class="evaluator-table"
      >
        <el-table-column prop="evaluatorName" label="评估器名称" width="220" fixed="left" :resizable="false">
          <template #default="{ row }">
            <OverflowTooltip
              :content="row.evaluatorName"
              class="linkish"
              role="button"
              tabindex="0"
              @click="editEvaluator(row)"
              @keyup.enter="editEvaluator(row)"
              @keyup.space.prevent="editEvaluator(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="evaluatorType" label="类型" min-width="130" :resizable="false" align="center">
          <template #default="{ row }">
            <OverflowTooltip :content="typeLabel(row.evaluatorType)" />
          </template>
        </el-table-column>
        <el-table-column prop="latestVersionName" label="最新版本" min-width="130" :resizable="false" align="center">
          <template #default="{ row }">
            <OverflowTooltip :content="row.latestVersionName || '-'" />
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="280" :resizable="false">
          <template #default="{ row }">
            <OverflowTooltip :content="row.description || '暂无描述'" />
          </template>
        </el-table-column>
        <el-table-column prop="createdByName" label="创建人" min-width="140" :resizable="false" align="center">
          <template #default="{ row }">
            <OverflowTooltip :content="row.createdByName || '-'" />
          </template>
        </el-table-column>
        <el-table-column prop="createdDate" min-width="190" :resizable="false" align="center">
          <template #header>
            <SortableHeader
              label="创建时间"
              field="createdDate"
              :sort-by="customSortBy"
              :sort-order="customSortOrder"
              @toggle="toggleCustomSort"
            />
          </template>
          <template #default="{ row }">
            <OverflowTooltip :content="formatTime(row.createdDate)" />
          </template>
        </el-table-column>
        <el-table-column prop="lastUpdatedByName" label="更新人" min-width="140" :resizable="false" align="center">
          <template #default="{ row }">
            <OverflowTooltip :content="row.lastUpdatedByName || '-'" />
          </template>
        </el-table-column>
        <el-table-column prop="lastUpdatedDate" min-width="190" :resizable="false" align="center">
          <template #header>
            <SortableHeader
              label="更新时间"
              field="lastUpdatedDate"
              :sort-by="customSortBy"
              :sort-order="customSortOrder"
              @toggle="toggleCustomSort"
            />
          </template>
          <template #default="{ row }">
            <OverflowTooltip :content="formatTime(row.lastUpdatedDate)" />
          </template>
        </el-table-column>
        <el-table-column column-key="actions" label="操作" width="180" fixed="right" :resizable="false" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="editEvaluator(row)">详情</el-button>
            <el-button link type="primary" @click.stop="openDescriptionDialog(row)">编辑</el-button>
            <el-button link type="danger" :loading="isDeletingEvaluator(row.id)" :disabled="isDeletingEvaluator(row.id)" @click="removeEvaluator(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager-row">
        <el-pagination
          v-model:current-page="customPage"
          v-model:page-size="customSize"
          :page-sizes="[5, 10, 20]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="customTotal"
          @size-change="changeCustomSize"
          @current-change="loadCustomEvaluators"
        />
      </div>
    </template>

    <template v-else>
      <div class="preset-layout evaluator-preset-layout">
        <aside class="preset-category-rail">
          <button
            v-for="category in categoryOptions"
            :key="category.id || 'preset-all'"
            :class="{ active: activeCategoryId === category.id }"
            @click="selectPresetCategory(category.id)"
          >
            {{ category.categoryName }}
          </button>
        </aside>
        <div class="preset-content">
          <div v-loading="presetLoading" class="preset-grid">
            <article
              v-for="preset in presetEvaluators"
              :key="preset.id"
              class="preset-card"
            >
              <div class="preset-card-head">
                <span class="preset-card-icon">
                  <el-icon><DataAnalysis /></el-icon>
                </span>
                <strong>{{ preset.evaluatorName }}</strong>
                <span class="preset-type-pill">{{ typeLabel(preset.evaluatorType) }}</span>
              </div>
              <p>{{ preset.description || '暂无描述' }}</p>
              <div class="preset-card-actions">
                <el-button link type="primary" :disabled="preset.evaluatorType === 'code'" @click.stop="createFromPreset(preset.id)">基于预置创建</el-button>
                <el-button link type="primary" :loading="isOpeningPreset(preset.id)" :disabled="isOpeningPreset(preset.id)" @click.stop="viewPreset(preset.id)">查看详情</el-button>
              </div>
            </article>
          </div>

          <div class="pager-row">
            <el-pagination
              v-model:current-page="presetPage"
              v-model:page-size="presetSize"
              :page-sizes="[5, 10, 20]"
              layout="total, sizes, prev, pager, next, jumper"
              :total="presetTotal"
              @size-change="changePresetSize"
              @current-change="loadPresetEvaluators"
            />
          </div>
        </div>
      </div>
    </template>
  </section>

  <el-dialog v-model="pickerVisible" title="创建评估器" class="evaluator-picker-dialog fixed-dialog" style="--fixed-dialog-width: min(1180px, 92vw); --fixed-dialog-height: min(720px, 84vh)" :close-on-click-modal="true">
    <div class="preset-layout picker-layout">
      <aside class="preset-category-rail">
        <span class="rail-caption">预置评估器分类</span>
        <button
          v-for="category in categoryOptions"
          :key="category.id || 'picker-all'"
          :class="{ active: pickerCategoryId === category.id }"
          @click="selectPickerCategory(category.id)"
        >
          {{ category.categoryName }}
        </button>
      </aside>
      <div class="preset-content">
        <div class="picker-head">
          <div>
            <h2>全部分类预置评估器</h2>
            <span class="meta">自定义创建评估器或选择预置评估器</span>
          </div>
          <div class="picker-actions">
            <el-input
              v-model="pickerKeyword"
              clearable
              placeholder="请输入名称"
              class="search-input"
              @keyup.enter="searchPicker"
              @clear="searchPicker"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-button class="search-icon-button" :icon="Search" title="搜索" aria-label="搜索" @click="searchPicker" />
            <el-button type="primary" :icon="Plus" @click="createCustom">自定义创建评估器</el-button>
          </div>
        </div>

        <div v-loading="pickerLoading" class="preset-grid picker-grid">
          <article
            v-for="preset in pickerPresets"
            :key="preset.id"
            class="preset-card"
          >
            <div class="preset-card-head">
              <span class="preset-card-icon">
                <el-icon><DataAnalysis /></el-icon>
              </span>
              <strong>{{ preset.evaluatorName }}</strong>
              <span class="preset-type-pill">{{ typeLabel(preset.evaluatorType) }}</span>
            </div>
            <p>{{ preset.description || '暂无描述' }}</p>
            <div class="preset-card-actions">
              <el-button link type="primary" :disabled="preset.evaluatorType === 'code'" @click.stop="createFromPreset(preset.id)">基于预置创建</el-button>
              <el-button link type="primary" :loading="isOpeningPreset(preset.id)" :disabled="isOpeningPreset(preset.id)" @click.stop="viewPreset(preset.id)">查看详情</el-button>
            </div>
          </article>
        </div>

        <div class="pager-row">
          <el-pagination
            v-model:current-page="pickerPage"
            v-model:page-size="pickerSize"
            :page-sizes="[5, 10, 20]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="pickerTotal"
            @size-change="changePickerSize"
            @current-change="loadPickerPresets"
          />
        </div>
      </div>
    </div>
  </el-dialog>

  <el-dialog v-model="detailVisible" :title="selectedPreset?.evaluatorName || '预置评估器详情'" class="preset-detail-dialog fixed-dialog" style="--fixed-dialog-width: min(900px, 86vw); --fixed-dialog-height: min(640px, 86vh)" :close-on-click-modal="true">
    <div v-loading="detailLoading" class="preset-detail">
      <template v-if="selectedPreset">
        <div class="detail-header-line">
          <span class="meta">{{ selectedPreset.description }}</span>
          <el-button type="primary" :disabled="selectedPreset.evaluatorType === 'code'" @click="createFromPreset(selectedPreset.id)">基于此预置评估器创建</el-button>
        </div>

        <template v-if="selectedPreset.evaluatorType === 'llm'">
          <h3>参数设置</h3>
          <el-table :data="selectedPreset.params" border>
            <el-table-column prop="paramName" label="变量名" min-width="180" :resizable="false">
              <template #default="{ row }">
                <OverflowTooltip :content="row.paramName || '-'" />
              </template>
            </el-table-column>
            <el-table-column prop="dataType" label="数据类型" width="120" :resizable="false">
              <template #default="{ row }">
                <OverflowTooltip :content="row.dataType || '-'" />
              </template>
            </el-table-column>
            <el-table-column label="是否必填" width="110" :resizable="false">
              <template #default="{ row }">
                <OverflowTooltip :content="row.required ? '是' : '否'" />
              </template>
            </el-table-column>
            <el-table-column prop="description" label="描述" min-width="360" :resizable="false">
              <template #default="{ row }">
                <OverflowTooltip :content="row.description || '-'" />
              </template>
            </el-table-column>
          </el-table>
          <h3>Prompt</h3>
          <pre class="code-block">{{ formatPromptBlock(selectedPreset.prompt) }}</pre>
        </template>
        <template v-else>
          <h3>代码入参设置</h3>
          <el-table :data="selectedPreset.params" border>
            <el-table-column prop="paramName" label="变量名" min-width="180" :resizable="false">
              <template #default="{ row }">
                <OverflowTooltip :content="row.paramName || '-'" />
              </template>
            </el-table-column>
            <el-table-column prop="dataType" label="数据类型" width="120" :resizable="false">
              <template #default="{ row }">
                <OverflowTooltip :content="row.dataType || '-'" />
              </template>
            </el-table-column>
            <el-table-column label="是否必填" width="110" :resizable="false">
              <template #default="{ row }">
                <OverflowTooltip :content="row.required ? '是' : '否'" />
              </template>
            </el-table-column>
            <el-table-column prop="description" label="描述" min-width="320" :resizable="false">
              <template #default="{ row }">
                <OverflowTooltip :content="row.description || '-'" />
              </template>
            </el-table-column>
            <el-table-column prop="defaultValue" label="默认值" min-width="180" :resizable="false">
              <template #default="{ row }">
                <OverflowTooltip :content="row.defaultValue || '-'" />
              </template>
            </el-table-column>
          </el-table>
          <h3>执行函数</h3>
          <pre class="code-block">{{ selectedPreset.executeCode }}</pre>
        </template>

        <div class="score-summary">
          <span>评分范围：{{ selectedPreset.scoreMin }} - {{ selectedPreset.scoreMax }}</span>
          <span>通过阈值：{{ selectedPreset.passThreshold }}</span>
        </div>
      </template>
    </div>
  </el-dialog>

  <ResourceDescriptionDialog
    v-model="descriptionDialogVisible"
    title="编辑评估器描述"
    name-label="评估器名称"
    :name="descriptionForm.name"
    :description="descriptionForm.description"
    :saving="descriptionSaving"
    @save="submitDescription"
  />
</template>
