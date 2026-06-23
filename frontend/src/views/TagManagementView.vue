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
  detailDialogVisible,
  detailLoading,
  tagDetail,
  detailPassOptions,
  detailFailOptions,
  editing,
  dialogTitle,
  tagForm,
  tagTypeOptions,
  booleanOptions,
  loadTags,
  openCreateDialog,
  openDetailDialog,
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

    <el-table v-loading="tagLoading" :data="tags" row-key="id" tooltip-effect="light" class="tag-table">
      <el-table-column prop="tagName" label="标签名称" min-width="260" show-overflow-tooltip>
        <template #default="{ row }">
          <span
            class="linkish"
            role="button"
            tabindex="0"
            @click="openDetailDialog(row)"
            @keyup.enter="openDetailDialog(row)"
            @keyup.space.prevent="openDetailDialog(row)"
          >
            {{ row.tagName }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="类型" width="160">
        <template #default="{ row }">
          <el-tag effect="plain">{{ getTagTypeLabel(row.tagType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="描述" min-width="300" show-overflow-tooltip>
        <template #default="{ row }">{{ row.description || '暂无描述' }}</template>
      </el-table-column>
      <el-table-column label="创建时间" width="210">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetailDialog(row)">详情</el-button>
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
      <el-form-item>
        <template #label>标签名称 <span class="required-mark">*</span></template>
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
      <el-form-item>
        <template #label>类型 <span class="required-mark">*</span></template>
        <el-select v-model="tagForm.tagType" class="wide-control" :disabled="editing">
          <el-option v-for="item in tagTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <span v-if="editing" class="hint">标签类型创建后不可修改</span>
      </el-form-item>

      <div v-if="tagForm.tagType === 'category'" class="tag-config-grid">
        <section class="option-group-card pass">
          <div class="option-group-head">
            <strong>Pass <span class="required-mark">*</span></strong>
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
            <strong>Fail <span class="required-mark">*</span></strong>
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
        <el-form-item>
          <template #label>评分范围 <span class="required-mark">*</span></template>
          <div class="range-row">
            <el-input-number v-model="tagForm.minValue" :min="1" :precision="0" controls-position="right" placeholder="请输入最小值" />
            <span>-</span>
            <el-input-number v-model="tagForm.maxValue" :min="1" :precision="0" controls-position="right" placeholder="请输入最大值" />
          </div>
        </el-form-item>
        <el-form-item>
          <template #label>
            通过阈值 <span class="required-mark">*</span>
            <span class="label-tip">大于等于该阈值为Pass</span>
          </template>
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

  <el-dialog v-model="detailDialogVisible" title="标签详情" width="760px" class="tag-dialog tag-detail-dialog">
    <div v-loading="detailLoading" class="tag-detail-body">
      <template v-if="tagDetail">
        <el-descriptions :column="2" border class="tag-detail-descriptions">
          <el-descriptions-item label="标签名称">{{ tagDetail.tagName }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ getTagTypeLabel(tagDetail.tagType) }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatTime(tagDetail.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ formatTime(tagDetail.lastUpdatedDate) }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ tagDetail.description || '暂无描述' }}</el-descriptions-item>
        </el-descriptions>

        <section v-if="tagDetail.tagType === 'category'" class="detail-section">
          <h3>标签选项</h3>
          <div class="detail-option-grid">
            <div class="detail-option-card pass">
              <strong>Pass</strong>
              <div class="detail-tag-list">
                <el-tag
                  v-for="(option, index) in detailPassOptions"
                  :key="option.id || `pass-${index}`"
                  type="success"
                  effect="plain"
                >
                  {{ option.optionName }}
                </el-tag>
                <span v-if="!detailPassOptions.length" class="meta">暂无配置</span>
              </div>
            </div>
            <div class="detail-option-card fail">
              <strong>Fail</strong>
              <div class="detail-tag-list">
                <el-tag
                  v-for="(option, index) in detailFailOptions"
                  :key="option.id || `fail-${index}`"
                  type="danger"
                  effect="plain"
                >
                  {{ option.optionName }}
                </el-tag>
                <span v-if="!detailFailOptions.length" class="meta">暂无配置</span>
              </div>
            </div>
          </div>
        </section>

        <section v-else-if="tagDetail.tagType === 'boolean'" class="detail-section">
          <h3>布尔选项</h3>
          <div class="detail-option-grid">
            <div class="detail-option-card pass">
              <strong>True</strong>
              <el-tag type="success" effect="plain">Pass</el-tag>
            </div>
            <div class="detail-option-card fail">
              <strong>False</strong>
              <el-tag type="danger" effect="plain">Fail</el-tag>
            </div>
          </div>
        </section>

        <section v-else-if="tagDetail.tagType === 'number'" class="detail-section">
          <h3>评分配置</h3>
          <div class="detail-metric-grid">
            <div class="detail-metric-card">
              <span class="meta">评分范围</span>
              <strong>{{ tagDetail.minValue }} - {{ tagDetail.maxValue }}</strong>
            </div>
            <div class="detail-metric-card">
              <span class="meta">通过阈值</span>
              <strong>≥ {{ tagDetail.passThreshold }}</strong>
            </div>
          </div>
        </section>

        <section v-else class="detail-section">
          <h3>文本配置</h3>
          <p class="meta">文本标签无需额外配置，评测人将在评测任务中填写文字评价。</p>
        </section>
      </template>
    </div>

    <template #footer>
      <el-button @click="detailDialogVisible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>
