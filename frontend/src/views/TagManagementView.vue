<script setup lang="ts">
import { Delete, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { useTagManagement } from '../modules/tag/composables/useTagManagement'

const {
  tagLoading,
  saving,
  tags,
  tagTotal,
  tagPage,
  tagSize,
  tagKeyword,
  tagType,
  dialogVisible,
  editing,
  dialogTitle,
  tagForm,
  tagTypeOptions,
  booleanOptions,
  loadTags,
  openCreateDialog,
  openEditDialog,
  submitTag,
  addCategoryOption,
  removeCategoryOption,
  getTagTypeLabel,
  formatTime
} = useTagManagement()
</script>

<template>
  <header class="topbar">
    <div>
      <p class="eyebrow">人工评测</p>
      <h1>标签管理</h1>
    </div>
    <div class="top-actions">
      <el-button :icon="Refresh" @click="loadTags">刷新</el-button>
      <el-button type="primary" :icon="Plus" @click="openCreateDialog">创建标签</el-button>
    </div>
  </header>

  <section class="tag-panel">
    <div class="panel-toolbar">
      <el-select v-model="tagType" clearable placeholder="全部类型" class="field-select" @change="loadTags">
        <el-option v-for="item in tagTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-input
        v-model="tagKeyword"
        clearable
        placeholder="请输入标签名称"
        class="search-input"
        maxlength="20"
        show-word-limit
        @keyup.enter="loadTags"
        @clear="loadTags"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-button @click="loadTags">搜索</el-button>
    </div>

    <el-table v-loading="tagLoading" :data="tags" row-key="id" class="tag-table">
      <el-table-column prop="tagName" label="标签名称" min-width="260">
        <template #default="{ row }">
          <span class="linkish">{{ row.tagName }}</span>
        </template>
      </el-table-column>
      <el-table-column label="类型" width="160">
        <template #default="{ row }">
          <el-tag effect="plain">{{ getTagTypeLabel(row.tagType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="描述" min-width="300">
        <template #default="{ row }">{{ row.description || '暂无描述' }}</template>
      </el-table-column>
      <el-table-column label="创建时间" width="210">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager-row">
      <el-pagination
        v-model:current-page="tagPage"
        v-model:page-size="tagSize"
        layout="total, prev, pager, next"
        :total="tagTotal"
        @current-change="loadTags"
      />
    </div>
  </section>

  <el-dialog v-model="dialogVisible" :title="dialogTitle" width="900px" class="tag-dialog" :close-on-click-modal="false">
    <el-form label-position="top" class="tag-form">
      <el-form-item label="标签名称 *">
        <el-input v-model="tagForm.tagName" maxlength="20" show-word-limit placeholder="请输入标签名称" />
      </el-form-item>
      <el-form-item label="描述">
        <el-input
          v-model="tagForm.description"
          type="textarea"
          maxlength="200"
          show-word-limit
          :autosize="{ minRows: 4, maxRows: 6 }"
          placeholder="请输入描述"
        />
      </el-form-item>
      <el-form-item label="类型 *">
        <el-select v-model="tagForm.tagType" class="wide-control" :disabled="editing">
          <el-option v-for="item in tagTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <span v-if="editing" class="hint">标签类型创建后不可修改</span>
      </el-form-item>

      <div v-if="tagForm.tagType === 'category'" class="tag-config-grid">
        <section class="option-group-card pass">
          <div class="option-group-head">
            <strong>Pass</strong>
            <el-button link type="primary" :icon="Plus" @click="addCategoryOption('pass')">添加标签</el-button>
          </div>
          <div class="option-list">
            <div v-for="(_, index) in tagForm.passOptions" :key="`pass-${index}`" class="option-editor">
              <el-input v-model="tagForm.passOptions[index]" maxlength="20" show-word-limit placeholder="请输入标签" />
              <el-button :icon="Delete" circle @click="removeCategoryOption('pass', index)" />
            </div>
          </div>
        </section>

        <section class="option-group-card fail">
          <div class="option-group-head">
            <strong>Fail</strong>
            <el-button link type="primary" :icon="Plus" @click="addCategoryOption('fail')">添加标签</el-button>
          </div>
          <div class="option-list">
            <div v-for="(_, index) in tagForm.failOptions" :key="`fail-${index}`" class="option-editor">
              <el-input v-model="tagForm.failOptions[index]" maxlength="20" show-word-limit placeholder="请输入标签" />
              <el-button :icon="Delete" circle @click="removeCategoryOption('fail', index)" />
            </div>
          </div>
        </section>
      </div>

      <div v-else-if="tagForm.tagType === 'boolean'" class="boolean-config">
        <div v-for="option in booleanOptions" :key="option.optionName" class="boolean-row">
          <span>{{ option.optionName }}</span>
          <el-tag :type="option.optionGroup === 'pass' ? 'success' : 'danger'" effect="plain">
            {{ option.optionGroup === 'pass' ? 'Pass' : 'Fail' }}
          </el-tag>
        </div>
      </div>

      <div v-else-if="tagForm.tagType === 'number'" class="number-config">
        <el-form-item label="评分范围">
          <div class="range-row">
            <el-input-number v-model="tagForm.minValue" :min="1" :precision="0" controls-position="right" placeholder="请输入最小值" />
            <span>-</span>
            <el-input-number v-model="tagForm.maxValue" :min="1" :precision="0" controls-position="right" placeholder="请输入最大值" />
          </div>
        </el-form-item>
        <el-form-item label="通过阈值 大于等于该阈值为Pass *">
          <el-input-number
            v-model="tagForm.passThreshold"
            :min="1"
            :precision="0"
            controls-position="right"
            class="wide-control"
            placeholder="请输入阈值"
          />
        </el-form-item>
      </div>

      <div v-else class="text-config">
        <span class="meta">文本标签无需额外配置，评测人将在评测任务中填写文字评价。</span>
      </div>
    </el-form>

    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submitTag">确定</el-button>
    </template>
  </el-dialog>
</template>
