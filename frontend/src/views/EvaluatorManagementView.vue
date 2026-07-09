<script setup>
import { Plus, Refresh, Search, Sort } from '@element-plus/icons-vue';
import { useEvaluatorManagement } from '../modules/evaluator/composables/useEvaluatorManagement';
const { activeTab, customLoading, customEvaluators, customTotal, customPage, customSize, customKeyword, customType, customSortBy, customSortOrder, columnWidths, categoryOptions, activeCategoryId, presetLoading, presetEvaluators, presetTotal, presetPage, presetSize, presetKeyword, pickerVisible, pickerCategoryId, pickerKeyword, pickerPage, pickerSize, pickerTotal, pickerLoading, pickerPresets, detailVisible, detailLoading, selectedPreset, loadCustomEvaluators, searchCustom, changeCustomSize, toggleCustomSort, loadPresetEvaluators, searchPreset, changePresetSize, selectPresetCategory, openPicker, loadPickerPresets, searchPicker, changePickerSize, selectPickerCategory, viewPreset, createCustom, createFromPreset, editEvaluator, removeEvaluator, handleColumnResize, typeLabel, formatTime } = useEvaluatorManagement();
</script>

<template>
  <header class="topbar">
    <div>
      <h1>评估器管理</h1>
    </div>
    <div class="top-actions">
      <el-button :icon="Refresh" @click="activeTab === 'custom' ? loadCustomEvaluators() : loadPresetEvaluators()">刷新</el-button>
      <el-button type="primary" :icon="Plus" @click="openPicker">创建评估器</el-button>
    </div>
  </header>

  <section class="evaluator-panel fill-workspace">
    <div class="evaluator-tabs">
      <button :class="{ active: activeTab === 'custom' }" @click="activeTab = 'custom'">自定义</button>
      <button :class="{ active: activeTab === 'preset' }" @click="activeTab = 'preset'">预置</button>
    </div>

    <template v-if="activeTab === 'custom'">
      <div class="panel-toolbar table-toolbar">
        <el-select v-model="customType" clearable placeholder="全部类型" class="field-select" @change="searchCustom">
          <el-option label="LLM" value="llm" />
          <el-option label="Code" value="code" disabled />
        </el-select>
        <el-input
          v-model="customKeyword"
          clearable
          placeholder="请输入评估器名称"
          class="search-input"
          @keyup.enter="searchCustom"
          @clear="searchCustom"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button @click="searchCustom">搜索</el-button>
        <div class="task-sort-actions">
          <el-button :class="{ active: customSortBy === 'lastUpdatedDate' }" :icon="Sort" @click="toggleCustomSort('lastUpdatedDate')">
            更新时间 {{ customSortBy === 'lastUpdatedDate' ? (customSortOrder === 'desc' ? '降序' : '升序') : '' }}
          </el-button>
          <el-button :class="{ active: customSortBy === 'createdDate' }" :icon="Sort" @click="toggleCustomSort('createdDate')">
            创建时间 {{ customSortBy === 'createdDate' ? (customSortOrder === 'desc' ? '降序' : '升序') : '' }}
          </el-button>
        </div>
      </div>

      <el-table
        v-loading="customLoading"
        :data="customEvaluators"
        row-key="id"
        border
        height="100%"
        tooltip-effect="light"
        class="evaluator-table"
        @header-dragend="handleColumnResize"
      >
        <el-table-column prop="evaluatorName" label="评估器名称" :width="columnWidths.evaluatorName" min-width="160" fixed="left" :resizable="false" show-overflow-tooltip />
        <el-table-column prop="evaluatorType" label="类型" :width="columnWidths.evaluatorType" min-width="100">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ typeLabel(row.evaluatorType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="latestVersionName" label="最新版本" :width="columnWidths.latestVersionName" min-width="110" />
        <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.description || '暂无描述' }}</template>
        </el-table-column>
        <el-table-column prop="createdByName" label="创建人" :width="columnWidths.createdByName" min-width="100" show-overflow-tooltip>
          <template #default="{ row }">{{ row.createdByName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="createdDate" label="创建时间" :width="columnWidths.createdDate" min-width="160">
          <template #default="{ row }">{{ formatTime(row.createdDate) }}</template>
        </el-table-column>
        <el-table-column prop="lastUpdatedByName" label="更新人" :width="columnWidths.lastUpdatedByName" min-width="100" show-overflow-tooltip>
          <template #default="{ row }">{{ row.lastUpdatedByName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="lastUpdatedDate" label="更新时间" :width="columnWidths.lastUpdatedDate" min-width="160">
          <template #default="{ row }">{{ formatTime(row.lastUpdatedDate) }}</template>
        </el-table-column>
        <el-table-column column-key="actions" label="操作" :width="columnWidths.actions" min-width="120" fixed="right" :resizable="false">
          <template #default="{ row }">
            <el-button link type="primary" @click="editEvaluator(row)">详情</el-button>
            <el-button link type="danger" @click="removeEvaluator(row)">删除</el-button>
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
      <div class="preset-layout">
        <aside class="preset-category-rail">
          <button
            v-for="category in categoryOptions"
            :key="category.id || 'all'"
            :class="{ active: activeCategoryId === category.id }"
            @click="selectPresetCategory(category.id)"
          >
            {{ category.categoryName }}
          </button>
        </aside>

        <div class="preset-content">
          <div class="panel-toolbar">
            <el-input
              v-model="presetKeyword"
              clearable
              placeholder="请输入预置评估器名称"
              class="search-input"
              @keyup.enter="searchPreset"
              @clear="searchPreset"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-button @click="searchPreset">搜索</el-button>
          </div>

          <div v-loading="presetLoading" class="preset-grid">
            <article v-for="preset in presetEvaluators" :key="preset.id" class="preset-card">
              <div class="preset-card-head">
                <span class="preset-mark"></span>
                <strong>{{ preset.evaluatorName }}</strong>
                <el-tag size="small" type="info">{{ typeLabel(preset.evaluatorType) }}</el-tag>
              </div>
              <p>{{ preset.description }}</p>
              <div class="preset-card-actions">
                <el-button type="primary" :disabled="preset.evaluatorType === 'code'" @click="createFromPreset(preset.id)">基于预置创建</el-button>
                <el-button @click="viewPreset(preset.id)">查看详情</el-button>
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

  <el-dialog v-model="pickerVisible" title="创建评估器" width="1180px" class="evaluator-picker-dialog fixed-dialog" :close-on-click-modal="true">
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
              placeholder="请输入预置评估器名称"
              class="search-input"
              @keyup.enter="searchPicker"
              @clear="searchPicker"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-button type="primary" :icon="Plus" @click="createCustom">自定义创建评估器</el-button>
          </div>
        </div>

        <div v-loading="pickerLoading" class="preset-grid picker-grid">
          <article v-for="preset in pickerPresets" :key="preset.id" class="preset-card">
            <div class="preset-card-head">
              <span class="preset-mark"></span>
              <strong>{{ preset.evaluatorName }}</strong>
              <el-tag size="small" type="info">{{ typeLabel(preset.evaluatorType) }}</el-tag>
            </div>
            <p>{{ preset.description }}</p>
            <div class="preset-card-actions">
              <el-button type="primary" :disabled="preset.evaluatorType === 'code'" @click="createFromPreset(preset.id)">基于预置创建</el-button>
              <el-button @click="viewPreset(preset.id)">查看详情</el-button>
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

  <el-dialog v-model="detailVisible" :title="selectedPreset?.evaluatorName || '预置评估器详情'" width="900px" class="preset-detail-dialog fixed-dialog" :close-on-click-modal="true">
    <div v-loading="detailLoading" class="preset-detail">
      <template v-if="selectedPreset">
        <div class="detail-header-line">
          <span class="meta">{{ selectedPreset.description }}</span>
          <el-button type="primary" :disabled="selectedPreset.evaluatorType === 'code'" @click="createFromPreset(selectedPreset.id)">基于此预置评估器创建</el-button>
        </div>

        <template v-if="selectedPreset.evaluatorType === 'llm'">
          <h3>参数设置</h3>
          <el-table :data="selectedPreset.params" border>
            <el-table-column prop="paramName" label="变量名" />
            <el-table-column prop="dataType" label="数据类型" width="120" />
            <el-table-column label="是否必填" width="110">
              <template #default="{ row }">{{ row.required ? '是' : '否' }}</template>
            </el-table-column>
            <el-table-column prop="description" label="描述" />
          </el-table>
          <h3>Prompt</h3>
          <pre class="code-block">{{ selectedPreset.prompt }}</pre>
        </template>
        <template v-else>
          <h3>代码入参设置</h3>
          <el-table :data="selectedPreset.params" border>
            <el-table-column prop="paramName" label="变量名" />
            <el-table-column prop="dataType" label="数据类型" width="120" />
            <el-table-column label="是否必填" width="110">
              <template #default="{ row }">{{ row.required ? '是' : '否' }}</template>
            </el-table-column>
            <el-table-column prop="description" label="描述" />
            <el-table-column prop="defaultValue" label="默认值" />
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
</template>
