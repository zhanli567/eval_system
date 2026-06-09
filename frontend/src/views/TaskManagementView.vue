<script setup lang="ts">
import { Plus, Refresh, Search, Sort } from '@element-plus/icons-vue'
import { useTaskManagement } from '../modules/task/composables/useTaskManagement'

const {
  loading,
  tasks,
  total,
  page,
  size,
  keyword,
  status,
  sortBy,
  sortOrder,
  statusOptions,
  loadTasks,
  searchTasks,
  openCreate,
  openDetail,
  startTask,
  terminateTask,
  removeTask,
  toggleSort,
  statusLabel,
  statusTagType,
  dimensionStatusLabel,
  formatRate,
  formatTime
} = useTaskManagement()
</script>

<template>
  <header class="topbar">
    <div>
      <p class="eyebrow">运行评测</p>
      <h1>评测任务</h1>
    </div>
    <div class="top-actions">
      <el-button :icon="Refresh" @click="loadTasks">刷新</el-button>
      <el-button type="primary" :icon="Plus" @click="openCreate">创建评测任务</el-button>
    </div>
  </header>

  <section class="task-panel">
    <div class="panel-toolbar task-toolbar">
      <el-select v-model="status" class="field-select" @change="searchTasks">
        <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-input
        v-model="keyword"
        clearable
        maxlength="50"
        show-word-limit
        placeholder="请输入任务名称"
        class="search-input"
        @keyup.enter="searchTasks"
        @clear="searchTasks"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-button @click="searchTasks">搜索</el-button>
      <div class="task-sort-actions">
        <el-button :class="{ active: sortBy === 'updatedAt' }" :icon="Sort" @click="toggleSort('updatedAt')">
          更新时间 {{ sortBy === 'updatedAt' ? (sortOrder === 'desc' ? '降序' : '升序') : '' }}
        </el-button>
        <el-button :class="{ active: sortBy === 'createdAt' }" :icon="Sort" @click="toggleSort('createdAt')">
          创建时间 {{ sortBy === 'createdAt' ? (sortOrder === 'desc' ? '降序' : '升序') : '' }}
        </el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="tasks" row-key="base.id" class="task-table" @row-click="openDetail">
      <el-table-column label="评测状态" width="130">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.base.status)" effect="plain">{{ statusLabel(row.base.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="任务名称" min-width="210">
        <template #default="{ row }">
          <span class="linkish">{{ row.base.taskName }}</span>
        </template>
      </el-table-column>
      <el-table-column label="评测集名称" min-width="180">
        <template #default="{ row }">
          <span>{{ row.base.datasetName }}</span>
          <el-tag size="small" effect="plain">{{ row.base.datasetVersionName }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="应用" min-width="150">
        <template #default="{ row }">
          {{ row.base.appType === 'agent' ? row.base.appId || '智能体应用' : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="评估器详情" min-width="240">
        <template #default="{ row }">
          <div v-if="row.evaluators.length" class="dimension-cell">
            <div class="dimension-chip">
              <strong>{{ row.evaluators[0].evaluatorName }}</strong>
              <span>{{ dimensionStatusLabel(row.evaluators[0].status) }} · 通过率 {{ formatRate(row.evaluators[0].passRate) }}</span>
            </div>
            <el-tag v-if="row.evaluators.length > 1" size="small">+{{ row.evaluators.length - 1 }}</el-tag>
          </div>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="标签详情" min-width="220">
        <template #default="{ row }">
          <div v-if="row.tags.length" class="dimension-cell">
            <div class="dimension-chip">
              <strong>{{ row.tags[0].tagName }}</strong>
              <span>{{ dimensionStatusLabel(row.tags[0].status) }} · 通过率 {{ formatRate(row.tags[0].passRate) }}</span>
            </div>
            <el-tag v-if="row.tags.length > 1" size="small">+{{ row.tags.length - 1 }}</el-tag>
          </div>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="评测时间" width="230">
        <template #default="{ row }">
          <div class="time-stack">
            <span>创建 {{ formatTime(row.base.createdAt) }}</span>
            <span>更新 {{ formatTime(row.base.updatedAt) }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="描述" min-width="220">
        <template #default="{ row }">{{ row.base.description || '暂无描述' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click.stop="openDetail(row)">详情</el-button>
          <el-button v-if="row.base.status !== 'completed' && row.base.status !== 'running'" link type="primary" @click.stop="startTask(row)">
            开始
          </el-button>
          <el-button v-if="row.base.status === 'running' || row.base.status === 'pending'" link type="warning" @click.stop="terminateTask(row)">
            终止
          </el-button>
          <el-button link type="danger" @click.stop="removeTask(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager-row">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        layout="total, prev, pager, next"
        :total="total"
        @current-change="loadTasks"
      />
    </div>
  </section>
</template>
